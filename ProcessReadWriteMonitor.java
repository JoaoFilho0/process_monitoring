import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ProcessReadWriteMonitor {

    private static final int PID = 5068; // Substitua pelo PID do processo desejado
    private static final long MONITOR_INTERVAL_MS = 1000; // Intervalo de monitoramento em milissegundos

    public static void main(String[] args) {
        System.out.println("Monitorando a taxa de leitura e escrita para o processo com PID " + PID);
        System.out.println("Intervalo de monitoramento: " + MONITOR_INTERVAL_MS + " ms\n");

        while (true) {
            try {
                long readBytesPrev = getProcessReadBytes(PID);
                long writeBytesPrev = getProcessWriteBytes(PID);

                Thread.sleep(MONITOR_INTERVAL_MS);

                long readBytesCurr = getProcessReadBytes(PID);
                long writeBytesCurr = getProcessWriteBytes(PID);

                long readRate = (readBytesCurr - readBytesPrev) / (MONITOR_INTERVAL_MS / 10000);
                long writeRate = (writeBytesCurr - writeBytesPrev) / (MONITOR_INTERVAL_MS / 10000);

                System.out.println("Taxa de Leitura: " + readRate + " bytes/10sec");
                System.out.println("Taxa de Escrita: " + writeRate + " bytes/10sec");
                System.out.println("----------------------------------------");
            } catch (IOException | InterruptedException e) {
                System.err.println("Erro ao ler os dados do processo " + PID + ": " + e.getMessage());
            }
        }
    }

    private static long getProcessReadBytes(int pid) throws IOException {
        String procFilePath = "/proc/" + pid + "/io";
        return getProcessIoStat(procFilePath, "read_bytes");
    }

    private static long getProcessWriteBytes(int pid) throws IOException {
        String procFilePath = "/proc/" + pid + "/io";
        return getProcessIoStat(procFilePath, "write_bytes");
    }

    private static long getProcessIoStat(String filePath, String statName) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(statName)) {
                    String[] parts = line.split("\\s+");
                    if (parts.length == 2) {
                        return Long.parseLong(parts[1]);
                    }
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
        return -1;
    }
}
