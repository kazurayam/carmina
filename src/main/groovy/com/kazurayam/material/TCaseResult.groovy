package com.kazurayam.material

import java.nio.file.Path
import java.time.LocalDateTime

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 *
 */
class TCaseResult implements Comparable<TCaseResult> {

    static Logger logger_ = LoggerFactory.getLogger(TCaseResult.class)

    private TSuiteResult parent_
    private TCaseName tCaseName_
    private Path tCaseDirectory_
    private List<Material> materials_
    private LocalDateTime lastModified_

    // --------------------- constructors and initializer ---------------------
    /**
     *
     * @param tCaseName
     */
    TCaseResult(TCaseName tCaseName) {
        tCaseName_ = tCaseName
        materials_ = new ArrayList<Material>()
        lastModified_ = LocalDateTime.MIN
    }

    // --------------------- properties getter & setters ----------------------
    TCaseResult setParent(TSuiteResult parent) {
        parent_ = parent
        tCaseDirectory_ = parent.getTSuiteTimestampDirectory().resolve(tCaseName_.getValue())
        return this
    }

    TSuiteResult getParent() {
        return parent_
    }

    TSuiteResult getTSuiteResult() {
        return this.getParent()
    }

    TCaseName getTCaseName() {
        return tCaseName_
    }

    Path getTCaseDirectory() {
        return tCaseDirectory_.normalize()
    }

    TCaseResult setLastModified(LocalDateTime lastModified) {
        lastModified_ = lastModified
        return this
    }

    LocalDateTime getLastModified() {
        return lastModified_
    }

    // --------------------- create/add/get child nodes ----------------------

    List<Material> getMaterials() {
        return materials_
    }

    List<Material> getMaterials(Path subpath, URL url, FileType fileType) {
        logger_.debug("#getMaterials subpath=${subpath.toString()}, url=${url.toString()}, fileType=${fileType.toString()}")
        List<Material> list = new ArrayList<Material>()
        logger_.debug("#getMaterials materials_.size()=${materials_.size()}")
        for (Material mate : materials_) {
            logger_.debug("#getMaterials mate.getSubpath()=${mate.getSubpath()}, mate.getURL()=${mate.getURL()}, mate.getFileType()=${mate.getFileType()}, mate.getPath()=${mate.getPath()}}")
            if (mate.getSubpath() == subpath &&
                mate.getURL().toString() == url.toString() &&
                mate.getFileType() == fileType) {
                list.add(mate)
            }
        }
        return list
    }


    Material getMaterial(Path subpath, URL url, Suffix suffix, FileType fileType) {
        for (Material mate : materials_) {
            if (mate.getURL().toString() == url.toString() &&
                mate.getSuffix() == suffix &&
                mate.getFileType() == fileType) {
                return mate
            }
        }
        return null
    }


    Material getMaterial(Path subpathUnderTCaseResult) {
        if (parent_ == null) {
            throw new IllegalStateException("parent_ is null")
        }
        List<Material> materials = this.getMaterials()
        //logger_.debug("#getMaterial materials.size()=${materials.size()}")
        for (Material mate : materials) {
            Path matePath = mate.getPath()
            Path subpath = this.getTCaseDirectory().relativize(matePath)
            logger_.debug("#getMaterial(Path) matePath=${matePath} subpath=${subpath} subpathUnderTCaseResult=${subpathUnderTCaseResult}")
            if (subpath == subpathUnderTCaseResult) {
                return mate
            }
        }
        return null
    }

    boolean addMaterial(Material material) {
        if (material.getParent() != this) {
            def msg = "material ${material.toJson()} does not have appropriate parent"
            logger_.error("#addMaterial ${msg}")
            throw new IllegalArgumentException(msg)
        }
        boolean found = false
        for (Material mate : materials) {
            if (mate == material) {
                found = true
            }
        }
        if (!found) {
            materials_.add(material)
            // sort the list materials by Material#compareTo()
            Collections.sort(materials_)
        }
        return found
    }

    // -------------------------- helpers -------------------------------------
    Suffix allocateNewSuffix(Path subpath, URL url, FileType fileType) {
        logger_.debug("#allocateNewSuffix subpath=${subpath.toString()}, url=${url.toString()}, fileType=${fileType.toString()}")
        List<Suffix> suffixList = new ArrayList<>()
        List<Material> mateList = this.getMaterials(subpath, url, fileType)
        logger_.debug("#allocateNewSuffix mateList.size()=${mateList.size()}")
        for (Material mate : mateList) {
            suffixList.add(mate.getSuffix())
        }
        Collections.sort(suffixList)
        logger_.debug("#allocateNewSuffix suffixList is ${suffixList.toString()}")
        Suffix newSuffix = null
        for (Suffix su : suffixList) {
            int next = su.getValue() + 1
            newSuffix = new Suffix(next)
            if (!suffixList.contains(newSuffix)) {
                return newSuffix
            }
        }
        return newSuffix
    }

    // ------------------ overriding Object properties ------------------------
    @Override
    boolean equals(Object obj) {
        //if (this == obj) {
        //    return true
        //}
        if (!(obj instanceof TCaseResult)) {
            return false
        }
        TCaseResult other = (TCaseResult) obj
        return tCaseName_ == other.getTCaseName()
    }

    @Override
    int hashCode() {
        return tCaseName_.hashCode()
    }

    @Override
    int compareTo(TCaseResult other) {
        return tCaseName_.compareTo(other.getTCaseName())
    }

    @Override
    String toString() {
        return this.toJson()
    }

    String toJson() {
        StringBuilder sb = new StringBuilder()
        sb.append('{"TCaseResult":{')
        sb.append('"tCaseName":"'   + Helpers.escapeAsJsonText(tCaseName_.toString())   + '",')
        sb.append('"tCaseDir":"'    + Helpers.escapeAsJsonText(tCaseDirectory_.toString())    + '",')
        sb.append('"materials":[')
        def count = 0
        for (Material mate : materials_) {
            if (count > 0) { sb.append(',') }
            sb.append(mate.toJson())
            count += 1
        }
        sb.append('],')
        sb.append('"lastModified":"' + lastModified_.toString() + '"')
        sb.append('}}')
        return sb.toString()
    }

    String toBootstrapTreeviewData() {
        StringBuilder sb = new StringBuilder()
        sb.append('{')
        sb.append('"text":"' + Helpers.escapeAsJsonText(tCaseName_.getValue())+ '",')
        sb.append('"selectable":false,')
        sb.append('"nodes":[')
        def mate_count = 0
        for (Material material : materials_) {
            if (mate_count > 0) {
                sb.append(',')
            }
            sb.append(material.toBootstrapTreeviewData())
            mate_count += 1
        }
        sb.append(']')
        if (this.getParent() != null && this.getParent().getJUnitReportWrapper() != null) {
            def status = this.getParent().getJUnitReportWrapper().getTestCaseStatus(this.getTCaseName().getId())
            sb.append(',')
            sb.append('"tags": ["')
            sb.append(status)
            sb.append('"]')
            /*
             * #1BC98E; green
             * #E64759; red
             * #9F86FF; purple
             * #E4D836; yellow
             */
            if (status == 'FAILED') {
                sb.append(',')
                sb.append('"backColor": "#E4D836"')
            } else if (status == 'ERROR') {
                sb.append(',')
                sb.append('"backColor": "#E64759"')
            }
        }
        sb.append('}')
        return sb.toString()
    }
}


