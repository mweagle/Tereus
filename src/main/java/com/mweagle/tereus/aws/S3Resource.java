// Copyright (c) 2015 Matt Weagle (mweagle@gmail.com)

// Permission is hereby granted, free of charge, to
// any person obtaining a copy of this software and
// associated documentation files (the "Software"),
// to deal in the Software without restriction,
// including without limitation the rights to use,
// copy, modify, merge, publish, distribute, sublicense,
// and/or sell copies of the Software, and to permit
// persons to whom the Software is furnished to do so,
// subject to the following conditions:

// The above copyright notice and this permission
// notice shall be included in all copies or substantial
// portions of the Software.

// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF
// ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
// TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
// PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT
// SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
// CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
// OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
// IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
package com.mweagle.tereus.aws;

import java.io.InputStream;
import java.util.Optional;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

/**
 * Created by mweagle on 5/7/15.
 */
public class S3Resource implements AutoCloseable {
    private final String bucketName;
    private final String keyName;
    private final InputStream inputStream;
    private final Optional<Long> inputStreamLength;
    
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

    public S3Resource(String bucketName, String keyName, InputStream is, Optional<Long> streamLength)
    {
        this.bucketName = bucketName;
        this.keyName = keyName;
        this.inputStream = is;
        this.inputStreamLength = streamLength;
        this.resourceURL = Optional.empty();
        this.released = false;
    }
    public boolean exists()
    {
        DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
        final AmazonS3Client awsClient = new AmazonS3Client(credentialProviderChain);
        try {
        	awsClient.getObjectMetadata(bucketName, getS3Path()); 
        } catch(AmazonServiceException e) {
            return false;
        }
        return true;  
    };
    
    public Optional<String> upload()
    {
        try {
            DefaultAWSCredentialsProviderChain credentialProviderChain = new DefaultAWSCredentialsProviderChain();
            final TransferManager transferManager = new TransferManager(
                    credentialProviderChain.getCredentials());

            final ObjectMetadata metadata = new ObjectMetadata();
            if (this.inputStreamLength.isPresent())
            {
            	metadata.setContentLength(this.inputStreamLength.get());
            }
            final PutObjectRequest uploadRequest = new PutObjectRequest(bucketName, keyName, this.inputStream, metadata);
            final Upload templateUpload = transferManager.upload(uploadRequest);

			templateUpload.waitForUploadResult();
            this.resourceURL = Optional.of(getS3Path());
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
    
    public String getS3Path()
    {
    	return String.format("https://s3.amazonaws.com/%s/%s", bucketName, keyName);
    }
}
