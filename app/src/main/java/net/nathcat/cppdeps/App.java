package net.nathcat.cppdeps;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class App {
  public static void main(String[] args) throws JsonSyntaxException, JsonIOException, IOException {
    Gson gson = new Gson();
    String[] dependencies = gson.fromJson(new InputStreamReader(new FileInputStream("dependencies.json")),
        String[].class);

    File outDir = new File("libs");
    Scanner input = new Scanner(System.in);

    if (outDir.exists()) {
      System.out
          .print("Directory 'libs' already exists. Proceed? Files in this directory may be overwritten! (y/n) > ");
      String in = input.nextLine().toLowerCase();
      if (in.equals("n")) {
        input.close();
        System.exit(0);
      }
    }

    if (!outDir.exists() && !outDir.mkdir()) {
      System.err.println("Failed to make directory: 'libs'");
      input.close();
      System.exit(1);
    }

    for (String artifact : dependencies) {
      byte[] content = download("https://cpp-deps.nathcat.net/" + artifact);
      System.out.println("\033[2K\r" + artifact + " - Downloaded");
      File f = new File(outDir, artifact);
      if (f.exists()) {
        System.out.println("File " + f.getPath() + " already exists!");
        System.exit(1);
      }

      if (!f.getParentFile().exists())
        f.getParentFile().mkdirs();

      f.createNewFile();

      FileOutputStream fos = new FileOutputStream(f);
      fos.write(content);
      fos.close();
    }

    input.close();
  }

  public static byte[] download(String url) {
    try {
      System.out.print("\n" + url + " - Starting download");

      HttpClient client = HttpClient.newHttpClient();
      HttpRequest.Builder b = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .GET();

      HttpRequest request = b.build();

      HttpResponse<byte[]> r = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

      System.out.print("\033[2K\r" + url + " - Downloading");

      if (r.statusCode() != 200) {
        throw new IOException(
            "Unable to download file '" + url + "': server replied with status code " + r.statusCode());
      }

      return r.body();
    } catch (IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}
