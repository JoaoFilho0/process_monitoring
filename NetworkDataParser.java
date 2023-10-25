import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetworkDataParser {

    public static void main(String[] args) {
        String data = "Inter-|   Receive                                                |  Transmit\n" +
                      " face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed\n" +
                      "   lo: 1542519    8908    0    0    0     0          0         0  1542519    8908    0    0    0     0       0          0\n" +
                      "enp1s0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0\n" +
                      "  wlo1: 19391613   70373    0    8    0     0          0         0  8846753   12572    0    0    0     0       0          0\n" +
                      "docker0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0";

        extractBytesForInterface(data, "wlo1");
    }

    public static void extractBytesForInterface(String data, String interfaceName) {
        String[] lines = data.split("\n");

        for (String line : lines) {
            if (line.contains(interfaceName)) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 10) {
                    float receivedBytes = Float.parseFloat(parts[2]);
                    float transmittedBytes = Float.parseFloat(parts[10]);
                    DecimalFormat format = new DecimalFormat("0.000");

                    System.out.println("Bytes Recebidos para " + interfaceName + ": " + format.format(receivedBytes / 1000000));
                    System.out.println("Bytes Transmitidos para " + interfaceName + ": " + format.format(transmittedBytes / 1000000));
                    return;
                }
            }
        }

        System.out.println("Interface não encontrada: " + interfaceName);
    }
}
