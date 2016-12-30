package lu.dainesch.huesense.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.StringTokenizer;
import lu.dainesch.huesense.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UPNPUtil {

    private static final Logger LOG = LoggerFactory.getLogger(UPNPUtil.class);

    private static final String UPNP_IP = "239.255.255.250";
    private static final int UPNP_PORT = 1900;
    private static final String UPNP_SEARCH = "M-SEARCH * HTTP/1.1\r\n"
            + "ST: urn:schemas-upnp-org:device:Basic:1\r\n"
            + "MX: 5\r\n"
            + "MAN: `ssdp:discover`\r\n".replace('`', '"')
            + "HOST: 239.255.255.250:1900\r\n\r\n";

    private UPNPUtil() {
    }

    public static String searchBridge() {

        try {
            byte[] send = UPNP_SEARCH.getBytes(StandardCharsets.UTF_8);
            byte[] rec;

            List<InetAddress> localAdrs = NetworkUtil.getLocalInetAddresses();
            if (localAdrs.isEmpty()) {
                return null;
            }
            for (InetAddress local : localAdrs) {

                try {
                    DatagramPacket sendPacket = new DatagramPacket(send, send.length, InetAddress.getByName(UPNP_IP), UPNP_PORT);

                    DatagramSocket clientSocket = new DatagramSocket(new InetSocketAddress(local.getHostAddress(), 0));
                    clientSocket.setSoTimeout(Constants.TIMEOUT.intValue());
                    clientSocket.send(sendPacket);

                    while (true) {
                        rec = new byte[1024];
                        DatagramPacket packet = new DatagramPacket(rec, rec.length);
                        clientSocket.receive(packet);

                        String response = new String(packet.getData());
                        StringTokenizer lineTok = new StringTokenizer(response, "\r\n");

                        boolean bridge = false;
                        String location = null;

                        while (lineTok.hasMoreTokens()) {
                            String line = lineTok.nextToken();

                            if (line.startsWith("LOCATION")) {
                                location = line;
                            } else if (line.startsWith("hue-bridgeid")) {
                                bridge = true;
                            }

                            if (bridge && location != null) {
                                String ip = location.replaceAll(".*://", "").replaceAll(":.*", "").replaceAll("/.*", "");
                                LOG.info("Found ip " + ip);
                                return ip;
                            }
                        }

                    }
                } catch (SocketTimeoutException ex) {
                    LOG.error("Bridge not found on IP " + local.toString());
                }
            }

        } catch (IOException ex) {
            LOG.error("Error searching bridge", ex);
        }
        return null;

    }



}
