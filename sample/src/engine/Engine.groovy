package engine

import ff.Invoker
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer

@CompileStatic
@Slf4j
class Engine {

    public static final short ev_eof = Invoker.ev_eof()
    public static final short ev_add = Invoker.ev_add()
    public static final short ev_delete = Invoker.ev_delete()
    public static final short evfilt_read = Invoker.evfilt_read()
    public static final short evfilt_write = Invoker.evfilt_write()

    // kq
    private int handleId

    Map<Integer, Sock> socks = [:]

    ArrayList<Timer> timers = []

    int init(String argv) {
        handleId = Invoker.init(argv)
        handleId
    }

    Timer t(long interval, Timer.Listener listener) {
        def timer = new Timer(System.nanoTime() + interval, interval, listener)
        timers.add(timer)
        timer
    }

    Sock.S createServer(int port, Sock.ServerListener listener) {
        def fd = Invoker.socket()
        if (fd < 0) {
            throw new IllegalStateException('create socket failed: ' + fd)
        }
        log.info 'create server sock fd: {}', fd

        def opt = Invoker.ioctl_nio(fd)
        if (opt < 0) {
            throw new IllegalStateException('ioctl nio failed: ' + opt)
        }
        log.info 'set nio: {}', opt

        def r = Invoker.bind(fd, port)
        if (r < 0) {
            throw new IllegalStateException('bind failed: ' + r)
        }
        log.info 'bind to port: {}', port

        def r2 = Invoker.listen(fd)
        if (r2 < 0) {
            throw new IllegalStateException('listen failed: ' + r2)
        }
        log.info 'listen on port: {}', port

        Invoker.evt_set_server_sock(fd, evfilt_read, ev_add)
        def r3 = Invoker.kevent(handleId, true, 0)
        if (r3 < 0) {
            throw new IllegalStateException('kevent post failed: ' + r3)
        }

        def sock = new Sock.S()
        sock.fd = fd
        sock.type = SockType.SERVER
        sock.listener = listener
        socks.put(fd, sock)
        sock
    }

    Sock.C createClient(String addr, int port, Sock.ClientListener listener) {
        def fd = Invoker.socket()
        if (fd < 0) {
            throw new IllegalStateException('create socket failed: ' + fd)
        }
        log.info 'create client sock fd: {}', fd

        def r = Invoker.connect(fd, addr, port)
        if (r < 0) {
            throw new IllegalStateException('connect failed: ' + r)
        }
        log.info 'connected to {}: {}', addr, port

        def opt = Invoker.ioctl_nio(fd)
        if (opt < 0) {
            log.warn 'ioctl nio failed: {}', opt
        }
        log.info 'set nio: {}', opt

        Invoker.evt_set(fd, evfilt_write, ev_add)
        def r2 = Invoker.kevent(handleId, true, 0)
        if (r2 < 0) {
            throw new IllegalStateException('kevent post failed: ' + r2)
        }

        def client = new Sock.C()
        client.fd = fd
        client.type = SockType.CLIENT
        client.listener = listener
        client.status = SockStatus.PENDING

        socks.put(fd, client)
        client
    }

    Sock getSock(int fd) {
        socks.get(fd)
    }

    void start(int bufSize = 1024) {
        def cb = new Loop(handleId, this, bufSize)
        Invoker.run(cb)
        log.info 'engine started'
    }
}
