
import java.io.*;
import java.net.URL;


/**
 * @author Andrew Tang
 */
public class NetworkUtility {

    public static File getFileFromURL(String filePath, String urlPath) throws IOException {
        URL url = new URL(urlPath);

        InputStream inputStream = url.openStream();
        BufferedInputStream bufferedStream = new BufferedInputStream(inputStream);

        File file = new File(filePath);
        FileOutputStream outputStream = new FileOutputStream(file);

        writeInputToOutput(bufferedStream, outputStream);

        return file;
    }

    public static void writeInputToOutput(InputStream inputStream, FileOutputStream outputStream) {
        byte[] buffer = new byte[1024];
        int bytesRead;
        long totalRead = 0;
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalRead += bytesRead;
                outputStream.write(buffer, 0, bytesRead);
            }
            inputStream.close();
            outputStream.close();
        }
        catch (IOException exception) { exception.printStackTrace(); }
    }
}
