import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class ProcessReadWriteStats {

    public static void main(String[] args) {
        int pid = 5068; // Substitua pelo PID do processo desejado

        try {
            long readBytes = getProcessReadBytes(pid);
            long writeBytes = getProcessWriteBytes(pid);

            System.out.println("Leitura de Bytes para o processo " + pid + ": " + readBytes);
            System.out.println("Escrita de Bytes para o processo " + pid + ": " + writeBytes);
        } catch (IOException e) {
            System.err.println("Erro ao ler os dados do processo " + pid + ": " + e.getMessage());
        }
    }

    public static long getProcessReadBytes(int pid) throws IOException {
        String procFilePath = "/proc/" + pid + "/io";
        return getProcessIoStat(procFilePath, "read_bytes");
    }

    public static long getProcessWriteBytes(int pid) throws IOException {
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
