import engine.Engine
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import org.slf4j.LoggerFactory
import sample.HttpResponseServer
import sample.PingPong

def log = LoggerFactory.getLogger(this.getClass())

// args override
def options = new Options()
options.addOption('s', 'server', false, 'start as ping/pong server')
options.addOption('p', 'port', true, 'server listen port')
options.addOption('c', 'client', false, 'start as ping/pong client')
options.addOption('a', 'addr', true, 'connect to server address, eg. 127.0.0.1')
options.addOption('i', 'procId', true, 'dpdk args: --proc-id')
options.addOption('t', 'procType', true, 'dpdk args: --proc-type')
options.addOption('h', 'http', false, 'start as http server')

def formatter = new HelpFormatter()
formatter.printHelp('please input follow args to run', options)

String[] argsGiven = super.binding.getProperty('args') as String[]
def parser = new DefaultParser()
def cmd = parser.parse(options, argsGiven)

def engine = new Engine()
def procId = cmd.hasOption('procId') ? cmd.getOptionValue('procId') : 0
def procType = cmd.hasOption('procType') ? cmd.getOptionValue('procType') : 'primary'
def handlerId = engine.init("--config=config.ini --proc-type=${procType} --proc-id=${procId}".toString())
if (handlerId < 0) {
    throw new IllegalStateException('init failed: ' + handlerId)
}

final int serverPort = cmd.hasOption('port') ? cmd.getOptionValue('port') as int : 12345
if (cmd.hasOption('server')) {
    if (cmd.hasOption('http')) {
        def http = new HttpResponseServer(engine)
        engine.createServer(serverPort, http)
    } else {
        def pp = new PingPong(engine)
        engine.createServer(serverPort, pp)
    }
    engine.start()
} else if (cmd.hasOption('client')) {
    if (!cmd.hasOption('addr')) {
        log.warn 'please input server address'
        return
    }
    def pp = new PingPong(engine)
    engine.createClient(cmd.getOptionValue('addr'), serverPort, pp)
    engine.start()
} else {
    log.warn 'unknown args'
}

