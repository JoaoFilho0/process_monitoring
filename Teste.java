import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
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
				System.out.println("Os dados serão medidos a cada 10 segundos até chegar em " + tempoDeMonitoramento + " minuto(s)");
				tempoDeMonitoramento *= 6;

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
		final int umMinutoEmMilissegundos = 10000;
		List<Float> listaRegistroMemoria = new ArrayList<>();
		List<Float> listaRegistroCpu = new ArrayList<>();
		String processInfo = null;

		for (int i = 1; i <= tempoDeMonitoramento; i++) {

			Process process = null;

			try {

				// econtra o processo pelo pid de acordo com o sistema operacional
				process = Runtime.getRuntime().exec("ps -p " + pid + " -o pid,%cpu,%mem");

				Process readBytesNetProcess = Runtime.getRuntime().exec("cat /proc/" + pid + "/net/dev");

				// pega as informações do processo
				Scanner scanner = new Scanner(process.getInputStream()).useDelimiter("\\A");
				processInfo = scanner.hasNext() ? scanner.next() : "";
				scanner.close();

				Scanner readByteNetScanner = new Scanner(readBytesNetProcess.getInputStream()).useDelimiter("\\A");
				String readedBytesProcess = readByteNetScanner.hasNext() ? readByteNetScanner.next() : "";
				readByteNetScanner.close();

				extractBytesForInterface(readedBytesProcess);

				if (processInfo.contains(Integer.toString(pid))) {

					System.out.println(processInfo);

					writeOnLinux(i, pid, processInfo, listaRegistroMemoria, listaRegistroCpu);

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
			writeResultOnLinux(listaRegistroMemoria, listaRegistroCpu);
		}
	}

	public static void writeResultOnLinux(List<Float> listaRegistroMemoria, List<Float> listaRegistroCpu) throws IOException {
		DecimalFormat formatador = new DecimalFormat("0.00");
		final int SLA_MIN_MEMORY = 1;
		final int SLA_MAX_MEMORY = 90; 
		final int SLA_AVERAGE_MEMORY = 45;

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

		float consumoMinimoDeMemoria = Collections.min(listaRegistroMemoria);
		float consumoMinimoDeCpu = Collections.min(listaRegistroCpu);

		float consumoMaximoDeMemoria = Collections.max(listaRegistroMemoria);
		float consumoMaximoDeCpu = Collections.max(listaRegistroCpu);

		float desvioPadraoDeMemoria = desvioPadrao(listaRegistroMemoria);
		float desvioPadraoDeCpu = desvioPadrao(listaRegistroCpu);

		System.out.println("Consumo minimo de memoria: " + formatador.format(consumoMinimoDeMemoria) + "% | SLA: " + SLA_MIN_MEMORY + "%" );
		System.out.println("Consumo minimo de CPU: " + formatador.format(consumoMinimoDeCpu) + "% | SLA: " + SLA_MIN_CPU + "%\n" );

		System.out.println("Consumo maximo de memoria: " + formatador.format(consumoMaximoDeMemoria) + "% | SLA: " + SLA_MAX_MEMORY + "%" );
		System.out.println("Consumo maximo de CPU: " + formatador.format(consumoMaximoDeCpu) + "% | SLA: " + SLA_MAX_CPU + "%\n" );

		System.out.println("Consumo medio de memoria: " + formatador.format(consumoMedioDeMemoria) + "% | SLA: " + SLA_AVERAGE_MEMORY + "%" );
		System.out.println("Consumo medio de CPU: " + formatador.format(consumoMedioDeCpu) + "% | SLA: " + SLA_AVERAGE_CPU + "%\n" );

		System.out.println("Desvio padrao de memoria: " + formatador.format(desvioPadraoDeMemoria) + " | SLA: " + SLA_STANDARD_DEVIATION + "%" );
		System.out.println("Desvio padrão de CPU: " + formatador.format(desvioPadraoDeCpu) + " | SLA: " + SLA_STANDARD_DEVIATION + "\n");

		BufferedWriter writer = new BufferedWriter(new FileWriter("arquivoDeMonitoramento.txt", true));
		writer.append("\n\nConsumo minimo de memoria: " + formatador.format(consumoMinimoDeMemoria) + "% | SLA: " + SLA_MIN_MEMORY + "%" );
		writer.append("\nConsumo maximo de memoria: " + formatador.format(consumoMaximoDeMemoria) + "% | SLA: " + SLA_MAX_MEMORY + "%" );
		writer.append("\nConsumo medio de memoria: " + formatador.format(consumoMedioDeMemoria) + "% | SLA: " + SLA_AVERAGE_MEMORY + "%" );
		writer.append("\nDesvio padrao de memoria: " + formatador.format(desvioPadraoDeMemoria) + " | SLA: " + SLA_STANDARD_DEVIATION + "%" );
		writer.append("\n\nConsumo minimo de CPU: " + formatador.format(consumoMinimoDeCpu) + "% | SLA: " + SLA_MIN_CPU );
		writer.append("\nConsumo maximo de CPU: " + formatador.format(consumoMaximoDeCpu) + "% | SLA: " + SLA_MAX_CPU);
		writer.append("\nConsumo medio de CPU: " + formatador.format(consumoMedioDeCpu) + "% | SLA: " + SLA_AVERAGE_CPU );
		writer.append("\nDesvio padrão de CPU: " + formatador.format(desvioPadraoDeCpu) + " | SLA: " + SLA_STANDARD_DEVIATION);
		writer.close();
	}

	public static void writeOnLinux(int indexDeEscrita, int pid, String processInforToBeWrite, List<Float> listaRegistroMemoria, List<Float> listaRegistroCpu) throws IOException {
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

	public static void extractBytesForInterface(String data) {
        String[] lines = data.split("\n");

        for (String line : lines) {
            if (line.contains(INTERFACE_NAME)) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 10) {
                    float receivedBytes = Float.parseFloat(parts[2]);
                    float transmittedBytes = Float.parseFloat(parts[10]);
                    DecimalFormat format = new DecimalFormat("0.000");

                    System.out.println("Bytes Recebidos para " + INTERFACE_NAME + ": " + format.format(receivedBytes / 1000000));
                    System.out.println("Bytes Transmitidos para " + INTERFACE_NAME + ": " + format.format(transmittedBytes / 1000000));
                    return;
                }
            }
        }

        System.out.println("Interface não encontrada: " + INTERFACE_NAME);
    }
}
