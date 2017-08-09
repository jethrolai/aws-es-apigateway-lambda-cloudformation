package com.jethrolai.api.aws;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * This is the place for your unit test
 * 
 * TODO implement all the tests!!!!!!
 *
 * @author jlai
 *
 */
public class ElasticSearchAWSStreamLambdaFacadeTest {

    private ElasticSearchAWSStreamLambdaFacade subject;
    private Context testContext;
    private InputStream testInput;
    private OutputStream testOutput;

    @Before
    public void setUp() throws Exception {
        subject = new ElasticSearchAWSStreamLambdaFacade();  
        testContext = mock(Context.class);
        testInput = mock(InputStream.class);
        testOutput = mock(OutputStream.class);
       
    }

    @Test
    public void should_handle_request() throws IOException {
        LambdaLogger mockLogger = mock(LambdaLogger.class);
        Mockito.when(testContext.getLogger()).thenReturn(mockLogger);
        subject.setUpLogger(testContext);
        Mockito.verify(testContext).getLogger();
        
    }
}