package com.cx.management.dto;

import lombok.Data;

import java.util.List;

@Data
public class MenuVO {

    private Long id;
    private Long parentId;
    private String name;
    private String permission;
    private Integer type;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private List<MenuVO> children;
}
