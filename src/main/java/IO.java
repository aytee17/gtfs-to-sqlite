import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Andrew Tang
 */
public class IO {

    public static File getFileFromURL(String filePath, String urlPath) throws IOException {
        URL url = new URL(urlPath);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();;

        File file = new File(filePath);
        int responseCode = httpConnection.getResponseCode();

        // HTTP redirect
        if (responseCode % 300 < 100) {
            String redirect = httpConnection.getHeaderField("Location");
            Main.print("Redirecting to " + redirect);
            return getFileFromURL(filePath, redirect);
        }
        // Everything else
        else if (responseCode != 200) {
            Main.print("HTTP " + responseCode + ": " + httpConnection.getResponseMessage());
            System.exit(-1);
        }

        int contentLength = httpConnection.getContentLength();
        double sizeInMb = (contentLength / ((double)(1024*1024)));
        double sizeInKb = (contentLength / 1024);

        double size = sizeInMb > 1 ? sizeInMb : sizeInKb;
        String unit = sizeInMb > 1 ? "MB" : "KB";

        Main.print("File size: " + size + unit);

        InputStream inputStream = httpConnection.getInputStream();
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
