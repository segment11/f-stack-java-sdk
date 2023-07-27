package engine

import ff.Invoker
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer

@CompileStatic
@Slf4j
class Loop extends Invoker.Callback {
    private int handlerId
    private Engine engine
    private int bufSize

    Loop(int handlerId, Engine engine, int bufSize) {
        this.handlerId = handlerId
        this.engine = engine
        this.bufSize = bufSize
    }

    private long loopCount = 0

    @Override
    int call(ByteBuffer args) {
        loopCount++
        if (loopCount % 100_000_000 == 0) {
            log.info 'loop count: {}', loopCount
        }

        int events = Invoker.kevent(handlerId, false, 0)
        int i = 0
        while (i < events) {
            int fd = Invoker.evt_fd(i)
            def s = engine.getSock(fd)
            switch (s.type) {
                case SockType.CLIENT: {
                    onClientEvent(i, (Sock.C) s)
                    break
                }
                case SockType.SERVER: {
                    onServerEvent(i, (Sock.S) s)
                    break
                }
            }
            i++
        }
        handlerTimers()
        0
    }

    private void handlerTimers() {
        long time = System.nanoTime()
        boolean isCancel = false
        for (t in engine.timers) {
            t.run(time)
            isCancel |= t.isCancel
        }
    }

    void onClientEvent(int index, Sock.C client) {
        try {
            if ((Invoker.evt_flags(index) & Engine.ev_eof) > 0) {
                Invoker.close(client.fd)
                log.info 'close client fd: {}', client.fd
                engine.socks.remove(client.fd)
                log.info 'remove client sock: {}', client.fd

                // server side
                if (client.server != null) {
                    client.server.listener.onClientDisconnect(client.server, client)
                } else {
                    // client side
                    client.listener.onDisconnect(client)
                }
            } else if (Invoker.evt_filter(index) == Engine.evfilt_read) {
                def recvBuf = ByteBuffer.allocateDirect(bufSize)
                def recv = Invoker.read(client.fd, recvBuf, bufSize)
                if (recv <= 0) {
                    return
                }

                // server side
                if (client.server != null) {
                    client.server.listener.onClientData(client.server, client, recvBuf, recv)
                } else {
                    // client side
                    client.listener.onData(client, recvBuf, recv)
                }
                // need release recvBuf?
            } else if (Invoker.evt_filter(index) == Engine.evfilt_write) {
                if (client.status == SockStatus.PENDING) {
                    client.status = SockStatus.CONNECTED
                    Invoker.evt_set(client.fd, Engine.evfilt_write, Engine.ev_delete)
                    Invoker.kevent(handlerId, true, 0)
                    Invoker.evt_set(client.fd, Engine.evfilt_read, Engine.ev_add)
                    Invoker.kevent(handlerId, true, 0)
                    client.listener.onConnect(client)
                }
            }
        } catch (Exception ex) {
            log.error('handle client event error, client fd: ' + client.fd, ex)
        }
    }

    void onServerEvent(int index, Sock.S server) {
        try {
            def x = Invoker.evt_filter(index)
            if (x != Engine.evfilt_read) {
                log.warn 'unknown server event, x: {}', x
                return
            }

            int available = Invoker.evt_data(index)
            do {
                int clientFd = Invoker.accept(server.fd)
                if (clientFd < 0) {
                    log.warn 'accept error, client fd: {}', clientFd
                    break
                }
                log.info 'accept client fd: {}', clientFd

                Invoker.evt_set(clientFd, Engine.evfilt_read, Engine.ev_add)
                def r = Invoker.kevent(handlerId, true, 0)
                if (r < 0) {
                    log.warn 'kevent post failed: {}', r
                    return
                }

                def client = new Sock.C()
                client.fd = clientFd
                client.type = SockType.CLIENT
                client.server = server
                client.status = SockStatus.CONNECTED
                engine.socks.put(clientFd, client)
                server.listener.onClientConnect(server, client)

                available--
            } while (available > 0)
        } catch (Exception ex) {
            log.error('handle server event error, server fd: ' + server.fd, ex)
        }
    }
}