package it.dt.function.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.Base64;

/**
 * @author Marco Picone, Ph.D. - picone.m@gmail.com
 * @project dt-fluid-function
 * @created 17/11/2023 - 15:30
 */
public class ImageTester {

    public static void main(String[] args) {

        try {

            String filePath = "data/test_image.png";
            String outputFileName = String.format("data/%d.png", System.currentTimeMillis());

            byte[] fileContent = FileUtils.readFileToByteArray(new File(filePath));
            String encodedString = Base64.getEncoder().encodeToString(fileContent);

            //System.out.println("Encoded String:" + encodedString);

            byte[] decodedBytes = Base64.getDecoder().decode(encodedString);
            FileUtils.writeByteArrayToFile(new File(outputFileName), decodedBytes);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

}
