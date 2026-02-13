package com.nazir.aiinvoice.domain.strategy;

import org.springframework.web.multipart.MultipartFile;

public interface StorageStrategy {

    String store(MultipartFile file);

}
