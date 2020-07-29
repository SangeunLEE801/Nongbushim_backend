package com.nongbushim;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@RestController
public class Controller {

    @GetMapping("/search")
    public String callApi() throws JsonProcessingException {
        RestTemplate restTemplate = new RestTemplate();
        String jsonInString = "";

        Map<String, Object> result = new HashMap<>();

        String url
                = "http://www.kamis.or.kr/service/price/xml.do?action=yearlySalesList";
        UriComponents uri = UriComponentsBuilder.fromHttpUrl(url
                +"&p_yyyy=2015&p_itemcategorycode=100&p_itemcode=111&p_kindcode=01&p_graderank=1&p_countycode=1101&p_convert_kg_yn=N&p_cert_key=111&p_cert_id=222&p_returntype=json"
        ).build();

        HttpHeaders header = new HttpHeaders();
        header.add("key","c870db87-9503-48c8-aca3-dee7f28a42ba");
        HttpEntity<?> entity = new HttpEntity<>(header);

        ResponseEntity<String> resultMap = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, String.class);
        result.put("statusCode", resultMap.getStatusCodeValue()); //http status code를 확인
        result.put("header", resultMap.getHeaders()); //헤더 정보 확인
        result.put("body", resultMap.getBody()); //실제 데이터 정보 확인

        //데이터를 제대로 전달 받았는지 확인 string형태로 파싱해줌
//        ObjectMapper mapper = new ObjectMapper();
        jsonInString = String.valueOf(result.get("body"));

        return jsonInString;
    }

}
