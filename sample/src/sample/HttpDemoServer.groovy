package sample

import engine.Sock
import engine.http.HeaderBody
import engine.http.HttpBaseServer
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.util.logging.Slf4j

import java.nio.ByteBuffer

@CompileStatic
@Slf4j
@InheritConstructors
class HttpDemoServer extends HttpBaseServer {
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

        log.info 'html buf size: {}', bufSize
    }

    @Override
    void doWithHeaderBody(Sock.C client, HeaderBody headerBody) {
        bufHtml.flip()
        client.write(bufHtml, bufSize)
    }
}
