package com.kazurayam.materials

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import com.kazurayam.materials.Helpers
import com.kazurayam.materials.MaterialRepository
import com.kazurayam.materials.MaterialRepositoryFactory
import com.kazurayam.materials.MaterialStorage
import com.kazurayam.materials.model.MaterialStorageImpl
import com.kazurayam.materials.model.TSuiteTimestampImpl

import spock.lang.Specification

class MaterialStorageSpec extends Specification {
    
    // fields
    static Logger logger_ = LoggerFactory.getLogger(MaterialStorageSpec);
    
    private static Path workdir_
    private static Path fixture_ = Paths.get("./src/test/fixture")
    private static MaterialRepository mr_
    
    // fixture methods
    def setupSpec() {
        workdir_ = Paths.get("./build/tmp/${Helpers.getClassShortName(MaterialStorageSpec.class)}")
        Helpers.copyDirectory(fixture_, workdir_)
        //
        mr_ = MaterialRepositoryFactory.createInstance(workdir_.resolve("Materials"))
    }
    def setup() {}
    def cleanup() {}
    def cleanupSpec() {}
    
    // feature methods
    def testBackup_specifyingTSuiteTimestamp() {
        setup:
        Path stepWork = workdir_.resolve("testBackup_specifyingTSuiteTimestamp")
        Path msdir = stepWork.resolve("Storage")
        MaterialStorage ms = MaterialStorageFactory.createInstance(msdir)
        when:
        int num = ms.backup(mr_, new TSuiteName("Monitor47News"), TSuiteTimestampImpl.newInstance("20190123_153854"))
        then:
        num == 1
    }
    
    def testRestore_specifyingTSuiteTimestamp() {
        setup:
        Path stepWork = workdir_.resolve("testRestore_specifyingTSuiteTimestamp")
        Path msdir = stepWork.resolve("Storage")
        Path restoredDir = stepWork.resolve("Materials_restored")
        MaterialStorage ms = MaterialStorageFactory.createInstance(msdir)
        when:
        int num = ms.backup(mr_, new TSuiteName("Monitor47News"), TSuiteTimestampImpl.newInstance("20190123_153854"))
        then:
        num == 1
        when:
        MaterialRepository restored = MaterialRepositoryFactory.createInstance(restoredDir)
        num = ms.restore(restored, new TSuiteName("Monitor47News"), TSuiteTimestampImpl.newInstance("20190123_153854"))
        then:
        num == 1
    }
    
}
