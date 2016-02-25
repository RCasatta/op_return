package io.geobit.opreturn;

import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.appengine.tools.cloudstorage.GcsFileOptions;
import com.google.appengine.tools.cloudstorage.GcsFilename;
import com.google.appengine.tools.cloudstorage.GcsInputChannel;
import com.google.appengine.tools.cloudstorage.GcsOutputChannel;
import com.google.appengine.tools.cloudstorage.GcsService;
import com.google.appengine.tools.cloudstorage.GcsServiceFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * Created by Riccardo Casatta @RCasatta on 30/06/15.
 *
 * Require cloud enabled. Go to appengine.google.com, go to Administration-> Application Settings. Scroll to the bottom of the page, Cloud Integration and enable it.
 * After a while you should see the Google Cloud Storage Bucket, in the "Basics" field in the same page.
 *
 * default bucket name equals to the appspot address (eg) appname.appspot.com
 *
 */
public class GCSService {
    private final static Logger log = Logger.getLogger(GCSService.class.getName());
    private final static GcsService gcsService = GcsServiceFactory.createGcsService();

    public static void writePlain(final String stringFileName, final String value)  {
        write(stringFileName,value,"text/plain");
    }

    public static void writeJs(final String stringFileName, final String value)  {
        write(stringFileName,value,"text/javascript; charset=utf-8");
    }

    public static void writeJson(final String stringFileName, final String value)  {
        write(stringFileName,value,"application/json; charset=utf-8");
    }

    public static void writeHtml(final String stringFileName, final String value)  {
        write(stringFileName,value,"text/html; charset=utf-8");
    }

    public static void writeXml(final String stringFileName, final String value)  {
        write(stringFileName,value,"application/xml; charset=utf-8");
    }

    private static void write(final String stringFileName, final String value, final String contentType)  {
        try {
            final GcsFileOptions options = new GcsFileOptions.Builder()
                    .mimeType(contentType)
                    .acl("public-read")
                    .build();

            final GcsOutputChannel writeChannel = gcsService.createOrReplace(open(stringFileName), options);
            final PrintWriter writer = new PrintWriter(Channels.newWriter(writeChannel, "UTF-8"));
            writer.print(value);
            writer.flush();
            writeChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getBucketRoot() {
        final String bucketName = AppIdentityServiceFactory.getAppIdentityService().getDefaultGcsBucketName();
        return String.format("http://storage.googleapis.com/%s", bucketName);
    }

    private static GcsFilename open(final String stringFileName) {
        final String bucketName = AppIdentityServiceFactory.getAppIdentityService().getDefaultGcsBucketName();
        log.info("opening " + String.format("http://storage.googleapis.com/%s/%s" , bucketName,stringFileName) );
        final GcsFilename filename = new GcsFilename(bucketName, stringFileName);
        return filename;
    }

    public static InputStream read(final String stringFileName) throws IOException {
        final GcsInputChannel readChannel = gcsService.openPrefetchingReadChannel( open(stringFileName) , 0, 1024 * 1024);
        return Channels.newInputStream(readChannel);
    }

    public static String readString(final String stringFileName) throws IOException {
        GcsFilename fileName = open(stringFileName);

        int fileSize = (int) gcsService.getMetadata(fileName).getLength();
        ByteBuffer result = ByteBuffer.allocate(fileSize);
        try (GcsInputChannel readChannel = gcsService.openReadChannel(fileName, 0)) {
            readChannel.read(result);
            String v = new String( result.array() , Charset.forName("UTF-8") );
            return v;
        }

    }

}
