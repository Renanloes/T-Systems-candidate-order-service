package com.tsystems.challenge.orders.service;

import com.tsystems.challenge.orders.dto.CreateOrderRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;

@Component
public class PricingApiClient {

    private final RestClient restClient;

    public PricingApiClient(
            RestClient.Builder restClientBuilder,
            @Value("${pricing.api.url}") String baseUrl
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();
    }

    public BigDecimal getPrice(CreateOrderRequest request) {
        try {
            PriceQuote quote = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/prices/{productId}")
                            .queryParam("country", request.country())
                            .queryParam("currency", request.currency())
                            .build(request.productId()))
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 404,
                            (response, body) -> {
                                throw new PricingApiException(
                                        "Product was not found in Pricing API",
                                        404
                                );
                            }
                    )
                    .onStatus(
                            status -> status.isError(),
                            (response, body) -> {
                                throw new PricingApiException(
                                        "Pricing API returned an error"
                                );
                            }
                    )
                    .body(PriceQuote.class);

            if (quote == null || quote.amount() == null) {
                throw new PricingApiException(
                        "Pricing API returned an invalid price quote"
                );
            }

            return new BigDecimal(quote.amount());

        } catch (PricingApiException ex) {
            throw ex;

        } catch (RestClientException | NumberFormatException ex) {
            throw new PricingApiException(
                    "Could not obtain price from Pricing API",
                    ex
            );
        }
    }

    private record PriceQuote(
            String quoteId,
            String productId,
            String country,
            String amount,
            String currency,
            String validUntil
    ) {
    }
}