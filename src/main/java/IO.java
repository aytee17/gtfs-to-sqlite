
import java.io.*;
import java.net.URL;

/**
 * @author Andrew Tang
 */
public class IO {

    public static File getFileFromURL(String filePath, String urlPath) throws IOException {
        URL url = new URL(urlPath);
        InputStream inputStream = url.openStream();
        File file = new File(filePath);
        writeInputToFile(inputStream, file);
        return file;
    }

    public static void writeInputToFile(InputStream inputStream, File outputFile) throws FileNotFoundException {
        BufferedInputStream bufferedInput = new BufferedInputStream(inputStream);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        BufferedOutputStream bufferedOutput = new BufferedOutputStream(fileOutputStream);

        byte[] buffer = new byte[1024];
        int bytesRead;
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                bufferedOutput.write(buffer, 0, bytesRead);
            }
            bufferedInput.close();
            bufferedOutput.close();
        }
        catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
