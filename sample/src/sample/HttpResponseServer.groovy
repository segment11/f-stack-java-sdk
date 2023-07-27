package sample

import engine.Engine
import engine.Sock
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer

@CompileStatic
@Slf4j
class HttpResponseServer implements Sock.ServerListener {
    private static ByteBuffer bufHtml = ByteBuffer.allocateDirect(1024)
    private static int bufSize = 1024

    static {
        String html = "HTTP/1.1 200 OK\r\n" +
                "Content-Length: 14\r\n" +
                "\r\n" +
                "<html>M</html>"

        def bytes = html.getBytes()
        bufHtml.put(bytes)
        bufSize = bytes.length
        bufHtml.flip()

        log.info 'html buf size: {}', bufSize
    }

    private Engine engine

    HttpResponseServer(Engine engine) {
        this.engine = engine
    }

    // server side
    @Override
    void onClientData(Sock.S server, Sock.C client, ByteBuffer buf, int len) {
        client.write(bufHtml, bufSize)
        bufHtml.flip()
    }
}
