package com.orthocarechain.dto.response;

import com.orthocarechain.enums.ImageType;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalImageResponse {
    private Long id;
    private Long reportId;
    private ImageType imageType;
    private String imageUrl;
    private String description;
    private String bodyPart;
    private String fileName;
    private Long fileSize;
    private LocalDateTime uploadedAt;
}
