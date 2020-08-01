package com.nongbushim;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchService {
    public List<String> searchAutoCompleteTarget(String term) throws IOException {
        InputStream resource = new ClassPathResource("list.txt").getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource));

        return reader.lines().filter(s -> s.contains(term)).collect(Collectors.toList());
    }

    public String searchKindCode(String item) throws IOException {
        InputStream resource = new ClassPathResource("listWithKindcode.txt").getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(resource));

        String line;
        while((line = reader.readLine()) != null){
            if (line.contains(item)) break;
        }
        String kindCode = line.substring(0,2);
        return kindCode;
    }
}
