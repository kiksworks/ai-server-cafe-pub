package ai_server_cafe.util.compressor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressHelper {
    public static String getCompressedString(@Nonnull String rawData) {
        byte[] input = rawData.getBytes(StandardCharsets.UTF_8);
        byte[] rawOutput = new byte[input.length];
        Deflater compressor = new Deflater();
        compressor.setInput(input);
        compressor.finish();
        int compressedDataLength = compressor.deflate(rawOutput);
        compressor.end();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(rawOutput, 0, compressedDataLength);
        Gson gson = new GsonBuilder().create();
        CompressedJsonString cj = new CompressedJsonString();
        cj.data = byteArrayOutputStream.toString(StandardCharsets.ISO_8859_1);
        cj.length = input.length;
        return gson.toJson(cj);
    }

    public static String getDeCompressedString(@Nonnull String compressedJson) {
        Gson gson = new GsonBuilder().create();
        CompressedJsonString cj = gson.fromJson(compressedJson, CompressedJsonString.class);
        byte[] input = cj.data.getBytes(StandardCharsets.ISO_8859_1);
        Inflater decompressor = new Inflater();
        decompressor.setInput(input, 0, input.length);
        byte[] rawResult = new byte[cj.length];
        int resultLength = 0;
        try {
            resultLength = decompressor.inflate(rawResult);
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
        decompressor.end();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(rawResult, 0, resultLength);
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }

    @Nonnull
    public static String getCompressedByte(@Nonnull String rawData) {
        byte[] input = rawData.getBytes(StandardCharsets.UTF_8);
        byte[] rawOutput = new byte[input.length];
        Deflater compressor = new Deflater();
        compressor.setInput(input);
        compressor.finish();
        int compressedDataLength = compressor.deflate(rawOutput);
        compressor.end();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(rawOutput, 0, compressedDataLength);
        byte[] data = byteArrayOutputStream.toByteArray();
        Gson gson = new GsonBuilder().create();
        CompressedJson cj = new CompressedJson();
        cj.data = data;
        cj.length = input.length;
        return gson.toJson(cj);
    }

    public static String getDeCompressedByte(@Nonnull String compressedJson) {
        Gson gson = new GsonBuilder().create();
        CompressedJson cj = gson.fromJson(compressedJson, CompressedJson.class);
        Inflater decompressor = new Inflater();
        byte[] input = cj.data;
        decompressor.setInput(input, 0, input.length);
        byte[] rawResult = new byte[cj.length];
        int resultLength = 0;
        try {
            resultLength = decompressor.inflate(rawResult);
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        }
        decompressor.end();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byteArrayOutputStream.write(rawResult, 0, resultLength);
        return byteArrayOutputStream.toString(StandardCharsets.UTF_8);
    }
}
