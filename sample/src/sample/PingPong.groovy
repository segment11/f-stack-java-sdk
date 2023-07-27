package sample

import engine.Engine
import engine.Sock
import engine.Timer
import ff.Invoker
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

@CompileStatic
@Slf4j
class PingPong implements Sock.ServerListener, Sock.ClientListener, Timer.Listener {
    private engine.Timer t

    private Sock.C client

    private Engine engine

    private ByteBuffer bufTimer = ByteBuffer.allocateDirect(1024)

    private final String QUIT = 'quit'

    PingPong(Engine engine) {
        this.engine = engine
    }

    // server side
    @Override
    void onClientData(Sock.S server, Sock.C client, ByteBuffer buf, int len) {
        log.info 'data received: {}', len

//        int bufSize = buf.limit() - buf.position()
        byte[] bytes = new byte[len]
        buf.get(bytes)
        def str = new String(bytes, StandardCharsets.UTF_8)
        if (str.contains(QUIT)) {
            log.info 'quit, client fd: {}', client.fd
            Invoker.close(client.fd)
            log.info 'client closed, fd: {}', client.fd
            engine.socks.remove(client.fd)
            log.info 'remove client sock: {}', client.fd
            return
        }

        buf.flip()
        int r = client.write(buf, len)
        if (r < 0) {
            log.warn 'write error: {}, fd: {}', r, client.fd
            Invoker.close(client.fd)
            log.info 'client closed, fd: {}', client.fd
            engine.socks.remove(client.fd)
            log.info 'remove client sock: {}', client.fd
        }
    }

    // client side
    @Override
    void onConnect(Sock.C client) {
        log.info('client connected, fd: {}', client.fd)
        if (t != null) {
            t.cancel()
        }
        this.client = client
        this.t = engine.t(1_000_000_000L, this)
    }

    @Override
    void onDisconnect(Sock.C client) {
        log.info('client disconnected, fd: {}', client.fd)
        if (t != null) {
            t.cancel()
            t = null
        }
        this.client = null
    }

    @Override
    void onData(Sock.C client, ByteBuffer buf, int len) {
        log.info 'data received: {}', len
    }

    private int count = 0

    @Override
    void trigger(Timer timer) {
        if (this.t == timer) {
            log.info 'count: {}', count++
            bufTimer.clear()
            def bytes = ('ping' + count).getBytes()
            bufTimer.put(bytes)
            bufTimer.flip()
            client.write(bufTimer, bytes.length)
        }
    }
}
