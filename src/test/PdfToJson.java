package test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

public class PdfToJson {

    private static final String[] CAMPOS = {
        "Nome do Funcionário:",
        "Número de Identificação:",
        "Cargo:",
        "Admissão:",
        "Departamento:",
        "Supervisor:",
        "Salário:",
        "Endereço:",
        "Telefone:",
        "Email",
        "Data de Nascimento:",
        "Estado Civil:",
        "Dependentes:",
        "Formação Acadêmica:",
        "Tipo de Documento:"
    };

    private static String[] valores = new String[CAMPOS.length];
    
    private static boolean isHolerite = false;

    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, SQLException {
        gerarTxt();
        efetuarUpdate();
        mensagemBonitinha();
    }

    public static synchronized void gerarTxt() {
        try {
            // Inicia a leitura do documento PDF
            File file = new File("D:\\DADOS DO USUARIO\\Desktop\\DADOS\\FUNCIONARIO1.pdf");
            PDDocument document = Loader.loadPDF(file);

            // Instancia um PDFTextStripper para extrair o texto do PDF
            PDFTextStripper textStripper = new PDFTextStripper();
            String pdfText = textStripper.getText(document);

            String outputPath = "D:\\DADOS DO USUARIO\\Desktop\\DADOS\\funcionario1.txt";
            File outputFile = new File(outputPath);
            FileWriter writer = new FileWriter(outputFile);
            writer.write(pdfText);
            writer.close();

            //System.out.println("Arquivo .txt gerado!");

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void efetuarUpdate() throws NoSuchAlgorithmException, SQLException {
    	
        try {
            JsonObjectBuilder jsonBuilder = Json.createObjectBuilder();
            BufferedReader reader = new BufferedReader(new FileReader("D:\\DADOS DO USUARIO\\Desktop\\DADOS\\funcionario1.txt"));
            String line;
             // Variável para verificar se é um holerite

            while ((line = reader.readLine()) != null) {
                for (int i = 0; i < CAMPOS.length; i++) {
                    if (line.startsWith(CAMPOS[i])) {
                        valores[i] = line.substring(CAMPOS[i].length()).trim();
                        break;
                    }
                }

                // Verificar se a linha indica um holerite
                if (line.startsWith("Tipo de Documento:")) {
                    String tipoDocumento = line.substring("Tipo de Documento:".length()).trim();
                    if (tipoDocumento.equalsIgnoreCase("Holerite")) {
                        isHolerite = true;
                    }
                }
            }

            for (int i = 0; i < CAMPOS.length; i++) {
                if (valores[i] != null) {
                    jsonBuilder.add(CAMPOS[i].replaceAll("\\s", "-").toLowerCase(), valores[i]);
                }
            }

            if (isHolerite) {
                jsonBuilder.add("tipo-de-documento", "holerite");
            } else {
                jsonBuilder.add("tipo-de-documento", "documento-x");
            }

            // Converte o JSON em uma string
            String input = jsonBuilder.toString();

            // Criamos uma instância do MessageDigest para calcular o hash SHA-256
            MessageDigest algorithm = MessageDigest.getInstance("SHA-256");

            byte messageDigest[] = algorithm.digest(input.getBytes("UTF-8"));
            String hash = bytesToHex(messageDigest);
            jsonBuilder.add("token", hash);
            
            // Aqui criamos o comando SQL para inserir o token na tabela "tokens_funcionarios"
            String sql = "INSERT INTO tokens_funcionario VALUES (?)";
            
            //Arquivo .properties para não expor dados sensíveis
			Properties properties = new Properties();
			FileInputStream in = new FileInputStream("D:\\DADOS DO USUARIO\\Desktop\\TUDO\\porcariada\\projetos\\cfgNorthWind\\config.properties");
	    	properties.load(in);
	    	
	    	String url = properties.getProperty("user");
	    	String user = properties.getProperty("url");
	    	String pass = properties.getProperty("password");
	    	
	    	Connection connection = DriverManager.getConnection(url, user, pass);
	    	PreparedStatement stmt = connection.prepareStatement(sql);
	    	stmt.setString(1, hash);
	    	
	    	
	    	// Verifica se tudo ocorreu bem
	    	int linhasAfetadas = stmt.executeUpdate();

            if (linhasAfetadas > 0) {
                System.out.println("Dados inseridos com sucesso!");
            } else {
                System.out.println("Nenhum dado inserido.");
            }

            JsonObject jsonObject = jsonBuilder.build();

            FileWriter jsonWriter = new FileWriter("D:\\DADOS DO USUARIO\\Desktop\\DADOS\\funcionario1.json");
            jsonWriter.write(jsonObject.toString());
            jsonWriter.close();
            reader.close();
            
            stmt.close();
            connection.close();

            //System.out.println("Arquivo JSON gerado com sucesso!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // Função para converter bytes em representação hexadecimal
    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte b : bytes) {
            hexStringBuilder.append(String.format("%02x", b));
        }
        return hexStringBuilder.toString();
    }
    
    private static void mensagemBonitinha() {
    	System.out.println("       _  _____  ____  _   _                            _                                                                 \r\n"
    			+ "      | |/ ____|/ __ \\| \\ | |                          | |                                                                \r\n"
    			+ "      | | (___ | |  | |  \\| |   __ _  ___ _ __ __ _  __| | ___     ___ ___  _ __ ___    ___ _   _  ___ ___  ___ ___  ___  \r\n"
    			+ "  _   | |\\___ \\| |  | | . ` |  / _` |/ _ \\ '__/ _` |/ _` |/ _ \\   / __/ _ \\| '_ ` _ \\  / __| | | |/ __/ _ \\/ __/ __|/ _ \\ \r\n"
    			+ " | |__| |____) | |__| | |\\  | | (_| |  __/ | | (_| | (_| | (_) | | (_| (_) | | | | | | \\__ \\ |_| | (_|  __/\\__ \\__ \\ (_) |\r\n"
    			+ "  \\____/|_____/ \\____/|_| \\_|  \\__, |\\___|_|  \\__,_|\\__,_|\\___/   \\___\\___/|_| |_| |_| |___/\\__,_|\\___\\___||___/___/\\___/ \r\n"
    			+ "                                __/ |                                                                                     \r\n"
    			+ "                               |___/                                                                                      ");
    }
    
    
}
