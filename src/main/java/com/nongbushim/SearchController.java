package com.nongbushim;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nongbushim.Dto.FormDto;
import com.nongbushim.Dto.GraphDto;
import com.nongbushim.Dto.ItemInfoDto;
import com.nongbushim.Dto.KamisRequestDto;
import com.nongbushim.Dto.KamisResponse.*;
import com.nongbushim.Enum.GradeRank;
import com.nongbushim.Enum.ItemCode;
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

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("form", new FormDto());
        return "index";
    }

    @PostMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public String search(@ModelAttribute("form") FormDto form, Model model) throws IOException {
        String input = form.getText();

        KamisRequestDto requestDto = convert(input);

        String url = "http://www.kamis.or.kr/service/price/xml.do?action=monthlySalesList";
        String parameters = "&p_yyyy=" + requestDto.getP_yyyy() + "&"
                + "p_period=" + requestDto.getP_period() + "&"
                + "p_itemcategorycode=" + requestDto.getP_itemcategorycode() + "&"
                + "p_itemcode=" + requestDto.getP_itemcode() + "&"
                + "p_kindcode=" + requestDto.getP_kindcode() + "&"
                + "p_graderank=" + requestDto.getP_graderank() + "&"
                + "p_countycode=" + requestDto.getP_countycode() + "&"
                + "p_convert_kg_yn=" + requestDto.getP_convert_kg_yn() + "&";
        UriComponents uri = UriComponentsBuilder.fromHttpUrl(url + parameters + "p_cert_key=111&p_cert_id=222&p_returntype=json").build();

        HttpHeaders header = new HttpHeaders();
        header.add("key", "c870db87-9503-48c8-aca3-dee7f28a42ba");
        HttpEntity<?> entity = new HttpEntity<>(header);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> resultMap = restTemplate.exchange(uri.toString(), HttpMethod.GET, entity, String.class);

        Gson gson = new Gson();
        KamisResponseSingleDto singleDto;
        KamisResponsePluralDto pluralDto;
        // 도매값 대상
        Price price;
        try{
            singleDto = gson.fromJson(resultMap.getBody(), KamisResponseSingleDto.class);
            price = singleDto.getPrice();
        } catch(JsonSyntaxException e){
            pluralDto = gson.fromJson(resultMap.getBody(), KamisResponsePluralDto.class);
            price = pluralDto.getPrice().get(0);
        }
        GraphDto graphDto = createGraphInfo(price);
        model.addAttribute("labels", graphDto.getLabel());
        model.addAttribute("data", graphDto.getMonthlySales());
        return "index";
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
}
