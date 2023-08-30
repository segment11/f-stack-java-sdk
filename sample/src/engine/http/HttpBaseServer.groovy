package engine.http

import engine.Engine
import engine.Sock
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer

@CompileStatic
@Slf4j
abstract class HttpBaseServer implements Sock.ServerListener {
    protected Engine engine

    HttpBaseServer(Engine engine) {
        this.engine = engine
    }

    private Map<Integer, LeftBuffer> leftBufByClientFd = [:]

    Long okCount = 0

    abstract void doWithHeaderBody(Sock.C client, HeaderBody headerBody)

    @Override
    void onClientDisconnect(Sock.S server, Sock.C client) {
        log.info 'client disconnected, fd: {}', client.fd
        leftBufByClientFd.remove(client.fd)
    }

    @Override
    void onClientData(Sock.S server, Sock.C client, ByteBuffer buf, int len) {
        def left = leftBufByClientFd.get(client.fd)
        if (left == null) {
            left = new LeftBuffer()
        }

        // direct byte buffer
        left.addBuffer(buf, 0, 0, len)

        int batchCount = 0
        do {
            def h = new HeaderBody()
            left = h.feedBuffer(null, left.length(), 0, left)

            if (left.isLastFeedOk) {
                batchCount++
                okCount++
                if (okCount % 1_000_000 == 0) {
                    log.info 'ok count: {}', okCount
                }

                doWithHeaderBody(client, h)
            }
        } while (left.isLastFeedOk && left.list.size() > 0)

        log.info 'one buffer batch count: {}', batchCount

        leftBufByClientFd.put(client.fd, left)
    }
}
