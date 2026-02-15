package com.nazir.aiinvoice.api.dto;

import java.util.List;

public class PagedResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PagedResponse(List<T> content, int page, int size, long totalElements, int totalPages) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public static <T> PagedResponseBuilder<T> builder() {
        return new PagedResponseBuilder<T>();
    }

    public static class PagedResponseBuilder<T> {
        private List<T> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;

        PagedResponseBuilder() {
        }

        public PagedResponseBuilder<T> content(List<T> content) {
            this.content = content;
            return this;
        }

        public PagedResponseBuilder<T> page(int page) {
            this.page = page;
            return this;
        }

        public PagedResponseBuilder<T> size(int size) {
            this.size = size;
            return this;
        }

        public PagedResponseBuilder<T> totalElements(long totalElements) {
            this.totalElements = totalElements;
            return this;
        }

        public PagedResponseBuilder<T> totalPages(int totalPages) {
            this.totalPages = totalPages;
            return this;
        }

        public PagedResponse<T> build() {
            return new PagedResponse<T>(content, page, size, totalElements, totalPages);
        }

        public String toString() {
            return "PagedResponse.PagedResponseBuilder(content=" + this.content + ", page=" + this.page + ", size=" + this.size + ", totalElements=" + this.totalElements + ", totalPages=" + this.totalPages + ")";
        }
    }
}
