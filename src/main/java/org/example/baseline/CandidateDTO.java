package org.example.baseline;

public class CandidateDTO {
    private final String fullyQualifiedClassName;
    private final boolean baselineFlag;
    private final boolean sonarFlag;
    private final boolean jdeodorantFlag;
    private final Integer wmcNullable;
    private final Double tccNullable;
    private final Integer atfdNullable;
    private final Integer cboNullable;
    private final Integer locNullable;
    private final int wmc;
    private final double tcc;
    private final int atfd;
    private final int cbo;
    private final int loc;
    private final boolean godClass;
    private final boolean usedCboFallback;
    private final int methodCount;
    private final int fieldCount;
    private final int dependencyTypeCount;
    private final java.util.List<String> reasons;

    public CandidateDTO(
            String fullyQualifiedClassName,
            int wmc,
            double tcc,
            int atfd,
            int cbo,
            int loc,
            boolean godClass,
            boolean usedCboFallback
    ) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.baselineFlag = false;
        this.sonarFlag = false;
        this.jdeodorantFlag = false;
        this.wmcNullable = wmc;
        this.tccNullable = tcc;
        this.atfdNullable = atfd;
        this.cboNullable = cbo;
        this.locNullable = loc;
        this.wmc = wmc;
        this.tcc = tcc;
        this.atfd = atfd;
        this.cbo = cbo;
        this.loc = loc;
        this.godClass = godClass;
        this.usedCboFallback = usedCboFallback;
        this.methodCount = 0;
        this.fieldCount = 0;
        this.dependencyTypeCount = 0;
        this.reasons = java.util.Collections.emptyList();
    }

    public CandidateDTO(
            String fullyQualifiedClassName,
            int methodCount,
            int fieldCount,
            int dependencyTypeCount,
            java.util.List<String> reasons
    ) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.baselineFlag = false;
        this.sonarFlag = false;
        this.jdeodorantFlag = false;
        this.wmcNullable = null;
        this.tccNullable = null;
        this.atfdNullable = null;
        this.cboNullable = null;
        this.locNullable = null;
        this.wmc = 0;
        this.tcc = 0.0;
        this.atfd = 0;
        this.cbo = 0;
        this.loc = 0;
        this.godClass = false;
        this.usedCboFallback = false;
        this.methodCount = methodCount;
        this.fieldCount = fieldCount;
        this.dependencyTypeCount = dependencyTypeCount;
        this.reasons = java.util.Collections.unmodifiableList(reasons);
    }

    public CandidateDTO(
            String fullyQualifiedClassName,
            boolean baselineFlag,
            boolean sonarFlag,
            boolean jdeodorantFlag,
            Integer wmc,
            Double tcc,
            Integer atfd,
            Integer cbo,
            Integer loc
    ) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.baselineFlag = baselineFlag;
        this.sonarFlag = sonarFlag;
        this.jdeodorantFlag = jdeodorantFlag;
        this.wmcNullable = wmc;
        this.tccNullable = tcc;
        this.atfdNullable = atfd;
        this.cboNullable = cbo;
        this.locNullable = loc;
        this.wmc = wmc == null ? 0 : wmc;
        this.tcc = tcc == null ? 0.0 : tcc;
        this.atfd = atfd == null ? 0 : atfd;
        this.cbo = cbo == null ? 0 : cbo;
        this.loc = loc == null ? 0 : loc;
        this.godClass = false;
        this.usedCboFallback = false;
        this.methodCount = 0;
        this.fieldCount = 0;
        this.dependencyTypeCount = 0;
        this.reasons = java.util.Collections.emptyList();
    }

    public CandidateDTO(
            String fullyQualifiedClassName,
            boolean baselineFlag,
            boolean sonarFlag,
            boolean jdeodorantFlag,
            Integer wmc,
            Double tcc,
            Integer atfd,
            Integer cbo,
            Integer loc,
            int methodCount,
            int fieldCount,
            int dependencyTypeCount,
            java.util.List<String> reasons
    ) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        this.baselineFlag = baselineFlag;
        this.sonarFlag = sonarFlag;
        this.jdeodorantFlag = jdeodorantFlag;
        this.wmcNullable = wmc;
        this.tccNullable = tcc;
        this.atfdNullable = atfd;
        this.cboNullable = cbo;
        this.locNullable = loc;
        this.wmc = wmc == null ? 0 : wmc;
        this.tcc = tcc == null ? 0.0 : tcc;
        this.atfd = atfd == null ? 0 : atfd;
        this.cbo = cbo == null ? 0 : cbo;
        this.loc = loc == null ? 0 : loc;
        this.godClass = false;
        this.usedCboFallback = false;
        this.methodCount = methodCount;
        this.fieldCount = fieldCount;
        this.dependencyTypeCount = dependencyTypeCount;
        this.reasons = java.util.Collections.unmodifiableList(reasons);
    }

    public static Builder builder(String fullyQualifiedClassName) {
        return new Builder(fullyQualifiedClassName);
    }

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public boolean isBaselineFlag() {
        return baselineFlag;
    }

    public boolean isSonarFlag() {
        return sonarFlag;
    }

    public boolean isJdeodorantFlag() {
        return jdeodorantFlag;
    }

    public Integer getWmcNullable() {
        return wmcNullable;
    }

    public Double getTccNullable() {
        return tccNullable;
    }

    public Integer getAtfdNullable() {
        return atfdNullable;
    }

    public Integer getCboNullable() {
        return cboNullable;
    }

    public Integer getLocNullable() {
        return locNullable;
    }

    public int getWmc() {
        return wmc;
    }

    public double getTcc() {
        return tcc;
    }

    public int getAtfd() {
        return atfd;
    }

    public int getCbo() {
        return cbo;
    }

    public int getLoc() {
        return loc;
    }

    public boolean isGodClass() {
        return godClass;
    }

    public boolean isUsedCboFallback() {
        return usedCboFallback;
    }

    public int getMethodCount() {
        return methodCount;
    }

    public int getFieldCount() {
        return fieldCount;
    }

    public int getDependencyTypeCount() {
        return dependencyTypeCount;
    }

    public java.util.List<String> getReasons() {
        return reasons;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        CandidateDTO that = (CandidateDTO) other;
        return baselineFlag == that.baselineFlag
                && sonarFlag == that.sonarFlag
                && jdeodorantFlag == that.jdeodorantFlag
                && wmc == that.wmc
                && Double.compare(that.tcc, tcc) == 0
                && atfd == that.atfd
                && cbo == that.cbo
                && loc == that.loc
                && godClass == that.godClass
                && usedCboFallback == that.usedCboFallback
                && methodCount == that.methodCount
                && fieldCount == that.fieldCount
                && dependencyTypeCount == that.dependencyTypeCount
                && java.util.Objects.equals(fullyQualifiedClassName, that.fullyQualifiedClassName)
                && java.util.Objects.equals(wmcNullable, that.wmcNullable)
                && java.util.Objects.equals(tccNullable, that.tccNullable)
                && java.util.Objects.equals(atfdNullable, that.atfdNullable)
                && java.util.Objects.equals(cboNullable, that.cboNullable)
                && java.util.Objects.equals(locNullable, that.locNullable)
                && java.util.Objects.equals(reasons, that.reasons);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                fullyQualifiedClassName,
                baselineFlag,
                sonarFlag,
                jdeodorantFlag,
                wmcNullable,
                tccNullable,
                atfdNullable,
                cboNullable,
                locNullable,
                wmc,
                tcc,
                atfd,
                cbo,
                loc,
                godClass,
                usedCboFallback,
                methodCount,
                fieldCount,
                dependencyTypeCount,
                reasons
        );
    }

    @Override
    public String toString() {
        return "CandidateDTO{"
                + "fullyQualifiedClassName='" + fullyQualifiedClassName + '\''
                + ", baselineFlag=" + baselineFlag
                + ", sonarFlag=" + sonarFlag
                + ", jdeodorantFlag=" + jdeodorantFlag
                + ", wmcNullable=" + wmcNullable
                + ", tccNullable=" + tccNullable
                + ", atfdNullable=" + atfdNullable
                + ", cboNullable=" + cboNullable
                + ", locNullable=" + locNullable
                + ", wmc=" + wmc
                + ", tcc=" + tcc
                + ", atfd=" + atfd
                + ", cbo=" + cbo
                + ", loc=" + loc
                + ", godClass=" + godClass
                + ", usedCboFallback=" + usedCboFallback
                + ", methodCount=" + methodCount
                + ", fieldCount=" + fieldCount
                + ", dependencyTypeCount=" + dependencyTypeCount
                + ", reasons=" + reasons
                + '}';
    }

    public static class Builder {
        private final String fullyQualifiedClassName;
        private boolean baselineFlag;
        private boolean sonarFlag;
        private boolean jdeodorantFlag;
        private Integer wmc;
        private Double tcc;
        private Integer atfd;
        private Integer cbo;
        private Integer loc;

        private Builder(String fullyQualifiedClassName) {
            this.fullyQualifiedClassName = fullyQualifiedClassName;
        }

        public Builder baselineFlag(boolean baselineFlag) {
            this.baselineFlag = baselineFlag;
            return this;
        }

        public Builder sonarFlag(boolean sonarFlag) {
            this.sonarFlag = sonarFlag;
            return this;
        }

        public Builder jdeodorantFlag(boolean jdeodorantFlag) {
            this.jdeodorantFlag = jdeodorantFlag;
            return this;
        }

        public Builder wmc(Integer wmc) {
            this.wmc = wmc;
            return this;
        }

        public Builder tcc(Double tcc) {
            this.tcc = tcc;
            return this;
        }

        public Builder atfd(Integer atfd) {
            this.atfd = atfd;
            return this;
        }

        public Builder cbo(Integer cbo) {
            this.cbo = cbo;
            return this;
        }

        public Builder loc(Integer loc) {
            this.loc = loc;
            return this;
        }

        public CandidateDTO build() {
            return new CandidateDTO(
                    fullyQualifiedClassName,
                    baselineFlag,
                    sonarFlag,
                    jdeodorantFlag,
                    wmc,
                    tcc,
                    atfd,
                    cbo,
                    loc
            );
        }
    }
}
