/*
 * ******************************************************************************
 *  * Copyright 2021 EMBL-EBI, Hinxton outstation
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *****************************************************************************
 */

package uk.ac.ebi.ena.backend.config;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.protocol.HttpContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;


@Slf4j
@Configuration
public class BeanConfig {

    public static int APP_RETRY = 3;

    @Bean
    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    @Bean
    public RequestConfig getRequestConfig() {
        return RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .setConnectTimeout(10 * 1000)
                .setSocketTimeout(60 * 1000)
                .build();
    }

    @Bean
    public HttpRequestRetryHandler getHttpRequestRetryHandler() {
        return new HttpRequestRetryHandler() {
            @SneakyThrows
            @Override
            public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
                // 3 retries at max
                if (executionCount > APP_RETRY) {
                    return false;
                } else {
                    // wait a second before retrying again
                    Thread.sleep(1000);
                    return true;
                }
            }
        };
    }

    @Bean
    public ServiceUnavailableRetryStrategy getServiceUnavailableRetryStrategy() {
        return
                new ServiceUnavailableRetryStrategy() {
                    @Override
                    public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
                        int statusCode = response.getStatusLine().getStatusCode();

                        // retry if status code is 502 or 503 and no more than three retries were done yet
                        return (statusCode == 502 || statusCode == 503) && executionCount < APP_RETRY;
                    }

                    @Override
                    public long getRetryInterval() {
                        return 1000;
                    }
                };
    }
}
