package engine

import ff.Invoker
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer

@CompileStatic
@Slf4j
class Sock {
    int fd
    SockType type

    @CompileStatic
    static class S extends Sock {
        ServerListener listener
    }

    @CompileStatic
    static class C extends Sock {
        Sock.S server
        SockStatus status
        ClientListener listener

        int write(java.nio.Buffer buf, int len) {
            if (SockStatus.CONNECTED != status) {
                throw new IllegalStateException('client not connected')
            }
            Invoker.write(fd, buf, len)
        }
    }

    @CompileStatic
    static interface ServerListener {
        default void onClientConnect(Sock.S server, Sock.C client) {
            Sock.log.info 'client connected, fd: {}', client.fd
        }

        default void onClientDisconnect(Sock.S server, Sock.C client) {
            Sock.log.info 'client disconnected, fd: {}', client.fd
        }

        void onClientData(Sock.S server, Sock.C client, java.nio.ByteBuffer buf, int len)
    }


    @CompileStatic
    static interface ClientListener {
        default void onConnect(Sock.C client) {
            Sock.log.info 'client connected, fd: {}', client.fd
        }

        default void onDisconnect(Sock.C client) {
            Sock.log.info 'client disconnected, fd: {}', client.fd
        }

        void onData(Sock.C client, java.nio.ByteBuffer buf, int len)
    }
}
