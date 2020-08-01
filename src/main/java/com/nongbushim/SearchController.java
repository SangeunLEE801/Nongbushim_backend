package com.nongbushim;

import com.nongbushim.Dto.ItemInfoDto;
import com.nongbushim.Dto.KamisRequestDto;
import com.nongbushim.Enum.GradeRank;
import com.nongbushim.Enum.ItemCode;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;

@RestController
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public String search(HttpServletRequest request) throws IOException {
        String input = request.getParameter("searchInput");

        KamisRequestDto requestDto = convert(input);

        RestTemplate restTemplate = new RestTemplate();
        String jsonInString = "";

        Map<String, Object> result = new HashMap<>();

        String parameters = "&p_yyyy=" + requestDto.getP_yyyy() + "&"
                + "p_period=" + requestDto.getP_period() + "&"
                + "p_itemcategorycode=" + requestDto.getP_itemcategorycode() + "&"
                + "p_itemcode=" + requestDto.getP_itemcode() + "&"
                + "p_kindcode=" + requestDto.getP_kindcode() + "&"
                + "p_graderank=" + requestDto.getP_graderank() + "&"
                + "p_countycode=" + requestDto.getP_countycode() + "&"
                + "p_convert_kg_yn=" + requestDto.getP_convert_kg_yn() + "&";
        String url
                = "http://www.kamis.or.kr/service/price/xml.do?action=monthlySalesList";
        UriComponents uri = UriComponentsBuilder.fromHttpUrl(url
                + parameters + "p_cert_key=111&p_cert_id=222&p_returntype=json"
        ).build();

        HttpHeaders header = new HttpHeaders();
        header.add("key", "c870db87-9503-48c8-aca3-dee7f28a42ba");
        HttpEntity<?> entity = new HttpEntity<>(header);

        ResponseEntity<String> resultMap = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, String.class);
        result.put("statusCode", resultMap.getStatusCodeValue()); //http status code를 확인
        result.put("header", resultMap.getHeaders()); //헤더 정보 확인
        result.put("body", resultMap.getBody()); //실제 데이터 정보 확인

        jsonInString = String.valueOf(result.get("body"));

        return jsonInString;
    }


    private KamisRequestDto convert(String input) throws IOException {
        ItemInfoDto itemInfoDto = searchInfo(input);
        return KamisRequestDto.builder()
                .p_yyyy("2020")
                .p_period("3")
                .p_itemcategorycode(itemInfoDto.getItemCategoryCode())
                .p_graderank(itemInfoDto.getGradeRank())
                .p_itemcode(itemInfoDto.getItemCode())
                .p_kindcode(itemInfoDto.getKindCode())
                .p_convert_kg_yn("N")
                .p_countycode("1101")
                .build();
    }

    private ItemInfoDto searchInfo(String input) throws IOException {
        String[] terms = input.split(" ");
        String itemName = terms[0];
        String kind = terms[1];
        String grade = terms[2];
        ItemInfoDto itemInfoDto = new ItemInfoDto();
        ItemCode itemCode = ItemCode.searchCode(itemName);
        itemInfoDto.setItemCode(itemCode.getCode());
        itemInfoDto.setItemCategoryCode(itemCode.getItemCategoryCode().getCode());
        itemInfoDto.setGradeRank(GradeRank.searchGradeRank(grade).getCode());
        itemInfoDto.setKindCode(service.searchKindCode(itemName + " " + kind));
        return itemInfoDto;
    }

    @RequestMapping("/nameAutoComplete")
    @ResponseBody
    public List<String> nameAutoComplete(@RequestParam(value = "term", required = false, defaultValue = "") String term) {
        List<String> suggestions = new LinkedList<>();
        try {
            return service.searchAutoCompleteTarget(term);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return suggestions;
    }
}
