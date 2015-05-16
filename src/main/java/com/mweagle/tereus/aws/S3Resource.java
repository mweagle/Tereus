package com.mweagle.tereus.aws;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import com.amazonaws.services.s3.transfer.model.UploadResult;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Optional;

/**
 * Created by mweagle on 5/7/15.
 */
public class S3Resource implements AutoCloseable {
    private final String bucketName;
    private final String baseKeyName;
    private final String data;
    public Optional<String> getResourceURL() {
        return resourceURL;
    }

    private Optional<String> resourceURL;
    private boolean released;

    public boolean isReleased() {
        return released;
    }

    public void setReleased(boolean released) {
        this.released = released;
    }


    public S3Resource(String bucketName, String baseKeyName, String data)
    {
        this.bucketName = bucketName;
        this.baseKeyName = baseKeyName;
        this.data = data;
        this.resourceURL = Optional.empty();
        this.released = false;
    }

    public Optional<String> upload()
    {
        final String templateDigest = DigestUtils.sha256Hex(this.data);
        final String keyName = String.format("%s-%s.cf.template", this.baseKeyName, templateDigest);
        try {
            DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
            final TransferManager transferManager = new TransferManager(
                    credentialProviderChain.getCredentials());

            final byte[] templateBytes = this.data.getBytes("UTF-8");
            final InputStream is = new ByteArrayInputStream(templateBytes);
            final ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(templateBytes.length);

            final PutObjectRequest uploadRequest = new PutObjectRequest(bucketName, keyName, is, metadata);
            final Upload templateUpload = transferManager.upload(uploadRequest);
            final UploadResult uploadResult = templateUpload.waitForUploadResult();
            this.resourceURL = Optional.of(String.format("https://s3.amazonaws.com/%s/%s", bucketName, keyName));
        }
        catch (Exception ex){
            throw new RuntimeException(ex);
        }
        return this.resourceURL;
    }
    @Override
    public void close() {
        if (!this.released && this.resourceURL.isPresent())
        {
            DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
            final AmazonS3Client awsClient = new AmazonS3Client(credentialProviderChain);
            final String[] parts = this.resourceURL.get().split("/");
            final String keyname = parts[parts.length-1];
            awsClient.deleteObject(bucketName, keyname);
        }
    }
}
