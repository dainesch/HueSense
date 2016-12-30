package lu.dainesch.huesense.net;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class NetworkUtil {

    public static List<InetAddress> getLocalInetAddresses() throws IOException {
        List<InetAddress> ret = new ArrayList<>();

        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        if (networkInterfaces == null) {
            return ret;
        }

        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface card = networkInterfaces.nextElement();

            if (card.isLoopback() || card.isPointToPoint() || card.isVirtual() || !card.isUp()) {
                continue;
            }

            Enumeration<InetAddress> addresses = card.getInetAddresses();
            while (addresses.hasMoreElements()) {
                InetAddress inetAddress = addresses.nextElement();
                int index = ret.size();

                if (Inet4Address.class.isInstance(inetAddress)) {
                    ret.add(index, inetAddress);
                }
            }
        }

        return ret;
    }

}
