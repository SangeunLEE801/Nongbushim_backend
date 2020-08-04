package com.nongbushim;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nongbushim.Dto.FormDto;
import com.nongbushim.Dto.GraphDto;
import com.nongbushim.Dto.ItemInfoDto;
import com.nongbushim.Dto.KamisRequestDto;
import com.nongbushim.Dto.KamisResponse.*;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.*;

@Controller
public class SearchController {
    private final static String URL = "http://www.kamis.or.kr/service/price/xml.do?action=monthlySalesList";
    private final static String FIXED_PARAMETERS = "p_cert_key=111&p_cert_id=222&p_returntype=json";
    private final static HttpHeaders HTTP_HEADERS;
    private final static HttpEntity<?> HTTP_ENTITY;
    private final static String ACCESS_KEY = "c870db87-9503-48c8-aca3-dee7f28a42ba";

    static {
        HTTP_HEADERS = new HttpHeaders();
        HTTP_HEADERS.add("key", ACCESS_KEY);
        HTTP_ENTITY = new HttpEntity<>(HTTP_HEADERS);
    }

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("form", new FormDto());
        return "index";
    }

    @RequestMapping("/autoComplete")
    @ResponseBody
    public List<String> autoComplete(@RequestParam(value = "term", required = false, defaultValue = "") String term) {
        List<String> suggestions = new LinkedList<>();
        try {
            return service.searchAutoCompleteTarget(term);
        } catch (IOException e) {
        }
        return suggestions;
    }

    @PostMapping(value = "/search")
    public String search(@ModelAttribute("form") FormDto form, Model model) throws IOException {
        String input = form.getText();
        String parameters = createParameters(input);

        UriComponents uri = UriComponentsBuilder.fromHttpUrl(URL + parameters + FIXED_PARAMETERS).build();
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resultMap = restTemplate.exchange(uri.toString(), HttpMethod.GET, HTTP_ENTITY, String.class);

        Price price = getWholesalePrice(resultMap);
        GraphDto graphDto = createGraphInfo(price);
        model.addAttribute("labels", graphDto.getLabel());
        model.addAttribute("data", graphDto.getMonthlySales());
        return "index";
    }

    private Price getWholesalePrice(ResponseEntity<String> resultMap) {
        Gson gson = new Gson();
        KamisResponseSingleDto singleDto;
        KamisResponsePluralDto pluralDto;
        // 도매값 대상
        Price price;
        try {
            singleDto = gson.fromJson(resultMap.getBody(), KamisResponseSingleDto.class);
            price = singleDto.getPrice();
        } catch (JsonSyntaxException e) {
            pluralDto = gson.fromJson(resultMap.getBody(), KamisResponsePluralDto.class);
            price = pluralDto.getPrice().get(0);
        }
        return price;
    }

    private String createParameters(String input) throws IOException {
        KamisRequestDto requestDto = convert(input);

        return "&p_yyyy=" + requestDto.getP_yyyy() + "&"
                + "p_period=" + requestDto.getP_period() + "&"
                + "p_itemcategorycode=" + requestDto.getP_itemcategorycode() + "&"
                + "p_itemcode=" + requestDto.getP_itemcode() + "&"
                + "p_kindcode=" + requestDto.getP_kindcode() + "&"
                + "p_graderank=" + requestDto.getP_graderank() + "&"
                + "p_countycode=" + requestDto.getP_countycode() + "&"
                + "p_convert_kg_yn=" + requestDto.getP_convert_kg_yn() + "&";
    }

    private GraphDto createGraphInfo(Price price) {
        GraphDto graphDto = new GraphDto();
        int lastItemIdx = price.getItem().size() - 1;

        String[] label = new String[12];
        String[] monthlySales = new String[12];
        int idx = 0;
        while (idx <= 11) {
            Item current = price.getItem().get(lastItemIdx);

            List<String> currentYearMonthlySalesList = currentYearMonthlySalesList(current);
            for (int i = 11; i >= 0 && idx <= 11; i--) {
                String sales = currentYearMonthlySalesList.get(i);
                if ("-".equals(sales)) continue;
                label[idx] = current.getYyyy() + "년-" + (i + 1) + "월";
                monthlySales[idx] = sales;
                idx++;
            }
            lastItemIdx--;

        }
        graphDto.setLabel(label);
        graphDto.setMonthlySales(monthlySales);
        return graphDto;
    }

    private List<String> currentYearMonthlySalesList(Item current) {
        return Arrays.asList(
                current.getM1(),
                current.getM2(),
                current.getM3(),
                current.getM4(),
                current.getM5(),
                current.getM6(),
                current.getM7(),
                current.getM8(),
                current.getM9(),
                current.getM10(),
                current.getM11(),
                current.getM12()
        );
    }

    private KamisRequestDto convert(String input) throws IOException {
        ItemInfoDto itemInfoDto = service.searchInfo(input);
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

}
