import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

public class Teste {

	static final String  INTERFACE_NAME = "wlo1";

	public static void main(String[] args) {
		int pid;
		int tempoDeMonitoramento;
		String osNome = System.getProperty("os.name");
		

		if (!osNome.startsWith("Linux")) {
			System.out.println(
					"O seu sistema operacional é desconheciso pelo o nosso sistema, algumas coisas podem não funcionar corretamente. Nome do Sistema Operacional:  "
							+ osNome);
		}

		// captura o pid do processo
		while (true) {
			System.out.println("Digite o pid do processo que deseja monitorar");
			Scanner scanner = new Scanner(System.in);

			if (scanner.hasNextInt()) {
				pid = scanner.nextInt();
				break;
			}

			System.out.println("pid invalido!");

		}

		// captura o tempo de monitoramento
		while (true) {
			System.out.println("Digite por quanto tempo (em minutos) deseja monitorar esse processo: ");
			Scanner scanner = new Scanner(System.in);

			if (scanner.hasNextInt()) {
				tempoDeMonitoramento = scanner.nextInt();
				System.out.println("Os dados serão medidos a cada 1 segundos até chegar em " + tempoDeMonitoramento + " minuto(s)");
				tempoDeMonitoramento *= 60;

				break;
			}

			System.out.println("tempo invalido! Digite apenas o minuto, não os segundos. Exemplo: 15");

		}

		try {
			showInformationProcess(pid, tempoDeMonitoramento);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void showInformationProcess(int pid, int tempoDeMonitoramento) throws IOException {
		DecimalFormat formatador = new DecimalFormat("0.00");
		final int umMinutoEmMilissegundos = 1000;
		List<Float> listaRegistroMemoria = new ArrayList<>();
		List<Float> listaRegistroCpu = new ArrayList<>();
		List<Float> listaRegistroNetRecebido = new ArrayList<>();
		List<Float> listaRegistroNetTransmitido = new ArrayList<>();
		List<Float> listaRegistroLeituraDeDisco = new ArrayList<>();
		List<Float> listaRegistroEscritaDeDisco = new ArrayList<>();
		String processInfo = null;
		long kilobyte = 1024;
		long megabyte = kilobyte * kilobyte;


		//Dados previos da leitura do Disco
		long readBytesIOPrev = getProcessIoStat("/proc/" + pid + "/io", "read_bytes");
		long writeBytesIOPrev = getProcessIoStat("/proc/" + pid + "/io", "write_bytes");


		//Dados previos da leitura da Internet
		Process readBytesNetProcess = Runtime.getRuntime().exec("cat /proc/" + pid + "/net/dev");

		Scanner readByteNetScanner = new Scanner(readBytesNetProcess.getInputStream()).useDelimiter("\\A");
		String readedBytesProcess = readByteNetScanner.hasNext() ? readByteNetScanner.next() : "";
		readByteNetScanner.close();

		String receivedAndTransmittedBytesNet = extractBytesForInterface(readedBytesProcess);
		String receivedNetPrevBytes = receivedAndTransmittedBytesNet.split("-")[0];
		String transmittedNetPrevBytes = receivedAndTransmittedBytesNet.split("-")[1];

			try {
				Thread.sleep(1000);

			} catch (InterruptedException e) {
				System.out.println("Algo deu errado!");
			}
		

		for (int i = 1; i <= tempoDeMonitoramento; i++) {
			
			Process process = null;

			try {

				
				// econtra o processo pelo pid de acordo com o sistema operacional
				process = Runtime.getRuntime().exec("ps -p " + pid + " -o pid,%cpu,%mem");


				// pega as informações do processo
				Scanner scanner = new Scanner(process.getInputStream()).useDelimiter("\\A");
				processInfo = scanner.hasNext() ? scanner.next() : "";
				scanner.close();

				//Dados atuais da leitura do Disco
				long readBytesIOCurrent = getProcessIoStat("/proc/" + pid + "/io", "read_bytes");
				long writeBytesIOCurrent = getProcessIoStat("/proc/" + pid + "/io", "write_bytes");


				//Dados atuais da leitura da Internet
				Process readBytesNetProcessCurrent = Runtime.getRuntime().exec("cat /proc/" + pid + "/net/dev");

				Scanner readByteNetScannerCurrent = new Scanner(readBytesNetProcessCurrent.getInputStream()).useDelimiter("\\A");
				String readedBytesProcessCurrent = readByteNetScannerCurrent.hasNext() ? readByteNetScannerCurrent.next() : "";
				readByteNetScannerCurrent.close();

				String receivedAndTransmittedBytesNetCurrent = extractBytesForInterface(readedBytesProcessCurrent);
				String receivedNetBytesCurrent = receivedAndTransmittedBytesNetCurrent.split("-")[0];
				String transmittedNetBytesCurrent = receivedAndTransmittedBytesNetCurrent.split("-")[1];
				
				float receveidNetBytes = (Long.parseLong(receivedNetBytesCurrent) - Long.parseLong(receivedNetPrevBytes)) / megabyte;
				float transmittedNetBytes = (Long.parseLong(transmittedNetBytesCurrent) - Long.parseLong(transmittedNetPrevBytes)) / megabyte;

				float writeBytesIO = (writeBytesIOCurrent - writeBytesIOPrev) / megabyte;
				float readBytesIO = (readBytesIOCurrent - readBytesIOPrev) / megabyte; 



				if (processInfo.contains(Integer.toString(pid))) {

					System.out.println(processInfo);
					System.out.println("Dados de disco escritos: " + formatador.format(writeBytesIO) + " MB");
					System.out.println("Dados de disco lidos: " + formatador.format(readBytesIO) + " MB");
					System.out.println("Dados de rede recebidos: " + formatador.format(receveidNetBytes) + " MB");
					System.out.println("Dados de rede transmitidos: " + formatador.format(transmittedNetBytes) + " MB");
					

					writeOnLinux(i, pid, processInfo, listaRegistroMemoria, listaRegistroCpu, 
					listaRegistroNetRecebido, listaRegistroNetTransmitido, listaRegistroLeituraDeDisco, listaRegistroEscritaDeDisco,
					 readBytesIO, writeBytesIO, receveidNetBytes, transmittedNetBytes);

					readBytesIOPrev = readBytesIOCurrent;
					writeBytesIOPrev = writeBytesIOCurrent;
					receivedNetPrevBytes = receivedNetBytesCurrent;
					transmittedNetPrevBytes = transmittedNetBytesCurrent;
				} else {
					System.out.println("O processo com PID " + pid + " não foi encontrado.");
					break;
				}

				
			} catch (IOException e) {
				System.out.println(
						"Ocorreu algum erro ao tentar capturar ou escrever as informações do processo no arquivo");
				break;

			} finally {
				if (process != null) {
					process.destroy();
				}
			}

			// tempo de espera até a proxima medição
			try {
				Thread.sleep(umMinutoEmMilissegundos);

			} catch (InterruptedException e) {
				System.out.println("Algo deu errado!");
			}
		}

		if(!listaRegistroMemoria.isEmpty() && !listaRegistroCpu.isEmpty()){
			writeResultOnLinux(listaRegistroMemoria, listaRegistroCpu, listaRegistroLeituraDeDisco, listaRegistroEscritaDeDisco, 
			listaRegistroNetRecebido, listaRegistroNetTransmitido);
		}
	}

	public static void writeResultOnLinux(List<Float> listaRegistroMemoria, List<Float> listaRegistroCpu, 
											List<Float> listaRegistroLeituraDeDisco, List<Float> listaRegistroEscritaDeDisco,
											List<Float> listaRegistroNetRecebido, List<Float> listaRegistroNetTransmitido) throws IOException {
		DecimalFormat formatador = new DecimalFormat("0.00");
		final int SLA_MIN_MEMORY = 1;
		final int SLA_MAX_MEMORY = 90; 
		final int SLA_AVERAGE_MEMORY = 45;

		final int SLA_MIN_OUTPUT_DISK = 1;
		final int SLA_MAX_OUTPUT_DISK = 10;
		final int SLA_AVERAGE_OUTPUT_DISK = 5;

		final int SLA_MIN_INPUT_DISK = 1;
		final int SLA_MAX_INPUT_DISK = 10;
		final int SLA_AVERAGE_INPUT_DISK = 5;

		final int SLA_MIN_RECEIVE_NET = 1;
		final int SLA_MAX_RECEIVE_NET = 10;
		final int SLA_AVERAGE_RECEIVE_NET = 5;

		final int SLA_MIN_TRANSMISSION_NET = 1;
		final int SLA_MAX_TRANSMISSION_NET = 10;
		final int SLA_AVERAGE_TRANSMISSION_NET = 5;

		final int SLA_MIN_CPU = 1;
		final int SLA_MAX_CPU = 98; 
		final int SLA_AVERAGE_CPU = 35;

		final int SLA_STANDARD_DEVIATION = 1;

		float consumoMedioDeMemoria = (float) listaRegistroMemoria
				.stream()
				.mapToDouble(Float::doubleValue)
				.average()
				.orElse(0.0);

		float consumoMedioDeCpu = (float) listaRegistroCpu
				.stream()
				.mapToDouble(Float::doubleValue)
				.average()
				.orElse(0.0);

		float taxaDeLeituraMediaDeDisco = (float) listaRegistroLeituraDeDisco
				.stream()
				.mapToDouble(Float::doubleValue)
				.average()
				.orElse(0.0);

		float taxaDeEscritaMediaDeDisco = (float) listaRegistroEscritaDeDisco
				.stream()
				.mapToDouble(Float::doubleValue)
				.average()
				.orElse(0.0);

		float taxaMediaDeDadosRecebidoPelaNet = (float) listaRegistroNetRecebido
				.stream()
				.mapToDouble(Float::doubleValue)
				.average()
				.orElse(0.0);

		float taxaDeTransmicaoMediaPelaNet = (float) listaRegistroNetTransmitido
				.stream()
				.mapToDouble(Float::doubleValue)
				.average()
				.orElse(0.0);

		float consumoMinimoDeMemoria = Collections.min(listaRegistroMemoria);
		float consumoMinimoDeCpu = Collections.min(listaRegistroCpu);
		float taxaDeLeituraMinimaDeDisco = Collections.min(listaRegistroEscritaDeDisco);
		float taxaDeEscritaMinimaDeDisco = Collections.min(listaRegistroEscritaDeDisco);
		float taxaMinimaDeDadosRecebidoPelaNet = Collections.min(listaRegistroNetRecebido);
		float taxaDeTransmicaoMinimaPelaNet = Collections.min(listaRegistroNetTransmitido);

		float consumoMaximoDeMemoria = Collections.max(listaRegistroMemoria);
		float consumoMaximoDeCpu = Collections.max(listaRegistroCpu);
		float taxaDeLeituraMaximaDeDisco = Collections.max(listaRegistroEscritaDeDisco);
		float taxaDeEscritaMaximaDeDisco = Collections.max(listaRegistroEscritaDeDisco);
		float taxaMaximaDeDadosRecebidoPelaNet = Collections.max(listaRegistroNetRecebido);
		float taxaDeTransmicaoMaximaPelaNet = Collections.max(listaRegistroNetTransmitido);

		float desvioPadraoDeMemoria = desvioPadrao(listaRegistroMemoria);
		float desvioPadraoDeCpu = desvioPadrao(listaRegistroCpu);
		float desvioPadraoDeLeituraDeDisco = desvioPadrao(listaRegistroLeituraDeDisco);
		float desvioPadraoDeEscritaDeDisco = desvioPadrao(listaRegistroEscritaDeDisco);
		float desvioPadraoDeRecebimentoPelaNet = desvioPadrao(listaRegistroNetRecebido);
		float desvioPadraoDeTransmicaoPelaNet = desvioPadrao(listaRegistroNetTransmitido);

		System.out.println("\n\nConsumo minimo de memoria: " + formatador.format(consumoMinimoDeMemoria) + "% | SLA: " + SLA_MIN_MEMORY + "%" );
		System.out.println("Consumo minimo de CPU: " + formatador.format(consumoMinimoDeCpu) + "% | SLA: " + SLA_MIN_CPU + "%" );
		System.out.println("Taxa de leitura mínima de disco: " + formatador.format(taxaDeLeituraMinimaDeDisco) + "MB | SLA: " + SLA_MIN_OUTPUT_DISK + "MB" );
		System.out.println("Taxa de escrita mínima de disco: " + formatador.format(taxaDeEscritaMinimaDeDisco) + "MB | SLA: " + SLA_MIN_INPUT_DISK + "MB" );
		System.out.println("Taxa de recebimento de dados pela net: " + formatador.format(taxaMinimaDeDadosRecebidoPelaNet) + "MB | SLA: " + SLA_MIN_RECEIVE_NET + "MB" );
		System.out.println("Taxa de transmissão de dados pela net: " + formatador.format(taxaDeTransmicaoMinimaPelaNet) + "MB | SLA: " + SLA_MIN_TRANSMISSION_NET + "MB\n" );

		System.out.println("Consumo maximo de memoria: " + formatador.format(consumoMaximoDeMemoria) + "% | SLA: " + SLA_MAX_MEMORY + "%" );
		System.out.println("Consumo maximo de CPU: " + formatador.format(consumoMaximoDeCpu) + "% | SLA: " + SLA_MAX_CPU + "%" );
		System.out.println("Taxa de leitura maximo de disco: " + formatador.format(taxaDeLeituraMaximaDeDisco) + "MB | SLA: " + SLA_MAX_OUTPUT_DISK + "MB" );
		System.out.println("Taxa de escrita máxima de disco: " + formatador.format(taxaDeEscritaMaximaDeDisco) + "MB | SLA: " + SLA_MAX_INPUT_DISK + "MB" );
		System.out.println("Taxa de recebimento máxima de dados pela net: " + formatador.format(taxaMaximaDeDadosRecebidoPelaNet) + "MB | SLA: " + SLA_MAX_RECEIVE_NET + "MB" );
		System.out.println("Taxa de transmissão máxima de dados pela net: " + formatador.format(taxaDeTransmicaoMaximaPelaNet) + "MB | SLA: " + SLA_MAX_TRANSMISSION_NET + "MB\n" );

		System.out.println("Consumo medio de memoria: " + formatador.format(consumoMedioDeMemoria) + "% | SLA: " + SLA_AVERAGE_MEMORY + "%" );
		System.out.println("Consumo medio de CPU: " + formatador.format(consumoMedioDeCpu) + "% | SLA: " + SLA_AVERAGE_CPU + "%" );
		System.out.println("Taxa de leitura médio de disco: " + formatador.format(taxaDeLeituraMediaDeDisco) + "MB | SLA: " + SLA_AVERAGE_OUTPUT_DISK + "MB" );
		System.out.println("Taxa de escrita médio de disco: " + formatador.format(taxaDeEscritaMediaDeDisco) + "MB | SLA: " + SLA_AVERAGE_INPUT_DISK + "MB" );
		System.out.println("Taxa de recebimento médio de dados pela net: " + formatador.format(taxaMediaDeDadosRecebidoPelaNet) + " MB| SLA: " + SLA_AVERAGE_RECEIVE_NET + "MB" );
		System.out.println("Taxa de transmissão médio de dados pela net: " + formatador.format(taxaDeTransmicaoMediaPelaNet) + "MB | SLA: " + SLA_AVERAGE_TRANSMISSION_NET + "MB\n" );

		System.out.println("Desvio padrao de memoria: " + formatador.format(desvioPadraoDeMemoria) + " | SLA: " + SLA_STANDARD_DEVIATION + "%" );
		System.out.println("Desvio padrão de CPU: " + formatador.format(desvioPadraoDeCpu) + " | SLA: " + SLA_STANDARD_DEVIATION + "");
		System.out.println("Desvio padrão de leitura de disco: " + formatador.format(desvioPadraoDeLeituraDeDisco) + "MB | SLA: " + SLA_STANDARD_DEVIATION + "MB" );
		System.out.println("Desvio padrão de escrita de disco: " + formatador.format(desvioPadraoDeEscritaDeDisco) + "MB | SLA: " + SLA_STANDARD_DEVIATION + "MB" );
		System.out.println("Desvio padrão de recebimento de dados pela net: " + formatador.format(desvioPadraoDeRecebimentoPelaNet) + "MB | SLA: " + SLA_STANDARD_DEVIATION + "MB" );
		System.out.println("Desvio padrão de transmissão dados pela net: " + formatador.format(desvioPadraoDeTransmicaoPelaNet) + "MB | SLA: " + SLA_STANDARD_DEVIATION + "MB\n" );

		BufferedWriter writer = new BufferedWriter(new FileWriter("arquivoDeMonitoramento.txt", true));
		writer.append("\n\nConsumo minimo de memoria: " + formatador.format(consumoMinimoDeMemoria) + "% | SLA: " + SLA_MIN_MEMORY + "%" );
		writer.append("\nConsumo maximo de memoria: " + formatador.format(consumoMaximoDeMemoria) + "% | SLA: " + SLA_MAX_MEMORY + "%" );
		writer.append("\nConsumo medio de memoria: " + formatador.format(consumoMedioDeMemoria) + "% | SLA: " + SLA_AVERAGE_MEMORY + "%" );
		writer.append("\nDesvio padrao de memoria: " + formatador.format(desvioPadraoDeMemoria) + " | SLA: " + SLA_STANDARD_DEVIATION + "%" );
		
		writer.append("\n\nConsumo minimo de CPU: " + formatador.format(consumoMinimoDeCpu) + "% | SLA: " + SLA_MIN_CPU + "%");
		writer.append("\nConsumo maximo de CPU: " + formatador.format(consumoMaximoDeCpu) + "% | SLA: " + SLA_MAX_CPU + "%");
		writer.append("\nConsumo medio de CPU: " + formatador.format(consumoMedioDeCpu) + "% | SLA: " + SLA_AVERAGE_CPU + "%");
		writer.append("\nDesvio padrão de CPU: " + formatador.format(desvioPadraoDeCpu) + " | SLA: " + SLA_STANDARD_DEVIATION + "%");
		
		writer.append("\n\nTaxa de leitura mínima de disco: " + formatador.format(taxaDeLeituraMinimaDeDisco) + "MB | SLA: " + SLA_MIN_CPU + "MB" );
		writer.append("\nTaxa de leitura maximo de disco: " + formatador.format(taxaDeLeituraMaximaDeDisco) + "MB | SLA: " + SLA_MAX_CPU + "MB" );
		writer.append("\nTaxa de leitura médio de disco: " + formatador.format(taxaDeLeituraMediaDeDisco) + "MB | SLA: " + SLA_MAX_CPU + "MB" );
		writer.append("\nDesvio padrão de leitura de disco: " + formatador.format(desvioPadraoDeLeituraDeDisco) + "MB | SLA: " + SLA_STANDARD_DEVIATION + "MB" );

		writer.append("\n\nTaxa de escrita mínima de disco: " + formatador.format(taxaDeEscritaMinimaDeDisco) + "MB | SLA: " + SLA_MIN_CPU + "MB" );
		writer.append("\nTaxa de escrita máxima de disco: " + formatador.format(taxaDeEscritaMaximaDeDisco) + "MB | SLA: " + SLA_MAX_CPU + "MB" );
		writer.append("\nTaxa de escrita médio de disco: " + formatador.format(taxaDeEscritaMediaDeDisco) + "MB | SLA: " + SLA_MAX_CPU + "MB" );
		writer.append("\nDesvio padrão de escrita de disco: " + formatador.format(desvioPadraoDeEscritaDeDisco) + "MB | SLA: " + SLA_STANDARD_DEVIATION + "MB" );

		writer.append("\n\nTaxa de recebimento de dados pela net: " + formatador.format(taxaMinimaDeDadosRecebidoPelaNet) + "MB | SLA: " + SLA_MIN_CPU + "MB" );
		writer.append("\nTaxa de recebimento máxima de dados pela net: " + formatador.format(taxaMaximaDeDadosRecebidoPelaNet) + "MB | SLA: " + SLA_MAX_CPU + "MB" );
		writer.append("\nTaxa de recebimento médio de dados pela net: " + formatador.format(taxaMediaDeDadosRecebidoPelaNet) + "MB | SLA: " + SLA_MAX_CPU + "MB" );
		writer.append("\nDesvio padrão de recebimento de dados pela net: " + formatador.format(desvioPadraoDeRecebimentoPelaNet) + "MB | SLA: " + SLA_STANDARD_DEVIATION + "MB" );

		writer.append("\n\nTaxa de transmissão de dados pela net: " + formatador.format(taxaDeTransmicaoMinimaPelaNet) + "MB | SLA: " + SLA_MIN_CPU + "MB" );
		writer.append("\nTaxa de transmissão máxima de dados pela net: " + formatador.format(taxaDeTransmicaoMaximaPelaNet) + "MB | SLA: " + SLA_MAX_CPU + "MB" );
		writer.append("\nTaxa de transmissão médio de dados pela net: " + formatador.format(taxaDeTransmicaoMediaPelaNet) + "MB | SLA: " + SLA_MAX_CPU + "MB" );
		writer.append("\nDesvio padrão de transmissão dados pela net: " + formatador.format(desvioPadraoDeTransmicaoPelaNet) + "MB | SLA: " + SLA_STANDARD_DEVIATION + "MB" );

		writer.close();
	}

	public static void writeOnLinux(int indexDeEscrita, 
	int pid, 
	String processInforToBeWrite, 
	List<Float> listaRegistroMemoria, 
	List<Float> listaRegistroCpu,
	List<Float> listaRegistroNetRecebido,
	List<Float> listaRegistroNetTransmitido,
	List<Float> listaRegistroLeituraDeDisco,
	List<Float> listaRegistroEscritaDeDisco,
	float readBytesIOPrev,
	float writeBytesIOPrev,
	float receivedNetBytes,
	float transmittedNetPrevBytes) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("arquivoDeMonitoramento.txt", indexDeEscrita != 1));

		if (indexDeEscrita == 1) {
			writer.write(
					"---------------------------------------------------- Relatorio de monitoramento do processo com o PID: "
							+ pid + " ---------------------------------------------------- \n");
			writer.append("\n" + processInforToBeWrite.split("\\r?\\n")[0] + "\n"
					+ processInforToBeWrite.split("\\r?\\n")[1]);

			writer.close();
		} else {
			writer.append(
					"\n" + processInforToBeWrite.split("\\r?\\n")[1]);

			writer.close();
		}

		String[] ultimaStringDividida = processInforToBeWrite.split("\\r?\\n")[1].split("\\s+");

		listaRegistroMemoria.add(Float.valueOf(ultimaStringDividida[3]));
		listaRegistroCpu.add(Float.valueOf(ultimaStringDividida[2]));
		listaRegistroNetRecebido.add(Float.valueOf(receivedNetBytes));
		listaRegistroNetTransmitido.add(Float.valueOf(transmittedNetPrevBytes));
		listaRegistroLeituraDeDisco.add(Float.valueOf(readBytesIOPrev));
		listaRegistroEscritaDeDisco.add(Float.valueOf(writeBytesIOPrev));
	}

	public static Float desvioPadrao(List<Float> array) {
		if (!checarItens(array)){
			Float soma = (float) 0, media = (float) 0;

			for (Float v : array) {
				soma += v;
			}

			media = soma / array.size();
			soma = (float) 0;

			for (Float v : array) {
				soma = (float) Math.pow((v - media), 2);
			}

			return (float) Math.sqrt(soma / array.size());
		} else {
			return (float) 0;
		}
	}

	public static Boolean checarItens(List<Float> array) {
		Boolean tudoIgual = true;

		for (int i = 0; i < array.size(); i++) {
			for (int j = 0; j < array.size(); j++) {
				if (!Objects.equals(array.get(i), array.get(j)) && i != j) {
					tudoIgual = false;
					break;
				}
			}
		}
		return tudoIgual;
	}

	public static String extractBytesForInterface(String data) {
		DecimalFormat format = new DecimalFormat("####.###");
        String[] lines = data.split("\n");
		long receivedBytes = 0;
		long transmittedBytes = 0;
        for (String line : lines) {
            if (line.contains(INTERFACE_NAME)) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 10) {
                    receivedBytes = Long.parseLong(parts[2]);
                    transmittedBytes = Long.parseLong(parts[10]);

                    break;
                }
            }
        }

		return format.format(receivedBytes) + "-" + format.format(transmittedBytes);
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
