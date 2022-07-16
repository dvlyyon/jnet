package org.dvlyyon.net.ssh;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Parameters;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;

public class LocalPF {
    public static void main(String... args)
            throws IOException {
        SSHClient ssh = new SSHClient();

        //ssh.loadKnownHosts();
        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        ssh.connect("localhost");
        try {

            ssh.authPublickey(System.getProperty("user.name"));

            /*
            * _We_ listen on localhost:8080 and forward all connections on to server, which then forwards it to
            * google.com:80
            */
            final Parameters params
                    = new Parameters("0.0.0.0", 8080, "google.com", 80);
            final ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(params.getLocalHost(), params.getLocalPort()));
            try {
                ssh.newLocalPortForwarder(params, ss).listen();
            } finally {
                ss.close();
            }

        } finally {
            ssh.disconnect();
        }
    }
}
