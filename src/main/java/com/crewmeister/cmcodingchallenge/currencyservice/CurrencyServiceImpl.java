package com.crewmeister.cmcodingchallenge.currencyservice;

import com.crewmeister.cmcodingchallenge.currency.CurrencyConstants;
import com.crewmeister.cmcodingchallenge.currency.CurrencyConversionRates;
import com.crewmeister.cmcodingchallenge.xmldata.GenericData;
import com.crewmeister.cmcodingchallenge.currency.Currency;
import com.crewmeister.cmcodingchallenge.currencyrepository.CurrencyRepository;
import com.crewmeister.cmcodingchallenge.xmldata.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CurrencyServiceImpl implements CurrencyService {


    private final RestTemplate restTemplate;

    @Value("${bundesbank.api.base.url}")
    private String bundesBaseUrl;

    @Value("${bundesbank.api.key}")
    private String bundesApiKey;

    @Value("${bundesbank.api.format}")
    private String bundesApiFormat;

    @Value("${bundesbank.api.lang}")
    private String bundesApiLang;


    public CurrencyServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    @Autowired
    CurrencyRepository currencyRepository;

    @Override
    public List<Currency> getListOfAvailableCurrencies() {
        return currencyRepository.findAll();
    }

    @Override
    public List<Currency> addCurrency(List<Currency> currency) {
        return currencyRepository.saveAll(currency);
    }

    @Override
    public Map<String, Map<String, String>> getFXRates(String date) {
        List<Currency> currencies = currencyRepository.findAll(); //Assuming DB has values; if not insert values from controller
        Map<String, Map<String, String>> fxMapResult = new HashMap<>();
        for(Currency fxcurrency: currencies) {
            System.out.println(fxcurrency.getCurrencyName());
            String url = buildUrl(bundesBaseUrl,bundesApiKey,bundesApiFormat,bundesApiLang);
            url=url.replace("XXX",fxcurrency.getCurrencyName());
            System.out.println("URL is "+url);
            ResponseEntity<GenericData> response = restTemplate.getForEntity(url, GenericData.class);
            GenericData genericData = response.getBody();
            Map<String, String> fxMap = new HashMap<>();


            for (Observation obs : genericData.dataSet.series.observations) {
                if (date == null) {
                    if (obs.value == null)
                        continue;
                    fxMap.put(obs.dimension.date, obs.value.rate);
                } else {
                    if (obs.dimension.date.equals(date) && obs.value != null) {
                        fxMap.put(obs.dimension.date, obs.value.rate);
                        break;
                    }
                    else if (obs.dimension.date.equals(date) && obs.value == null) {
                        fxMap.put(date, "Rate cannot be fetched as it is weekend or public holiday");
                        break;
                    }
                }
            }
            fxMapResult.put(fxcurrency.getCurrencyName(), fxMap);
        }

        return fxMapResult;
    }

    @Override
    public double getFXAmount(String date, String currency, double amount) {

        System.out.println(currency);
        CurrencyConversionRates ccr = null;
        UriComponentsBuilder queryParams = UriComponentsBuilder.newInstance();
        queryParams.queryParam("format","sdmx");
        queryParams.queryParam("lang","en");
        String url = buildUrl(bundesBaseUrl,bundesApiKey,bundesApiFormat,bundesApiLang);
        url=url.replace("XXX",currency);
        System.out.println("URL is "+url);
        ResponseEntity<GenericData> response = restTemplate.getForEntity(url, GenericData.class);
        GenericData genericData = response.getBody();

        for (Observation obs : genericData.dataSet.series.observations) {
            if (obs.dimension.date.equals(date) && obs.value != null) {
                ccr = new CurrencyConversionRates(Double.parseDouble(obs.value.rate));
                break;
            }
        }
        System.out.println(ccr.getConversionRate());
        double exchangedAmount = amount/ccr.getConversionRate() ;

        return exchangedAmount;
    }

    private String buildUrl(String baseUrl, String pathSegment, String format, String lang) {
        return UriComponentsBuilder
                .fromHttpUrl(baseUrl.endsWith("/") ? baseUrl : baseUrl + "/")
                .path(pathSegment.startsWith("/") ? pathSegment.substring(1) : pathSegment)
                .queryParam("format", format)
                .queryParam("lang", lang)
                .build()
                .toUriString();
    }


}
