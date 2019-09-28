package com.kazurayam.materials.view


import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.kazurayam.materials.Helpers
import com.kazurayam.materials.Material
import com.kazurayam.materials.VTLoggerEnabled
import com.kazurayam.materials.imagedifference.ComparisonResult
import com.kazurayam.materials.repository.RepositoryVisitor

import groovy.xml.MarkupBuilder

import java.nio.file.Path

class RepositoryVisitorGeneratingHtmlDivsAsModalConcise
    extends RepositoryVisitorGeneratingHtmlDivsAsModalBase {

    static Logger logger_ = LoggerFactory.getLogger(
        RepositoryVisitorGeneratingHtmlDivsAsModalConcise.class)

    String classShortName = Helpers.getClassShortName(
        RepositoryVisitorGeneratingHtmlDivsAsModalConcise.class)

    /**
    * Constructor
    *
    * @param mkbuilder
    */
    RepositoryVisitorGeneratingHtmlDivsAsModalConcise(MarkupBuilder mkbuilder) {
        super(mkbuilder)
    }

    @Override String getBootstrapModalSize() {
        return 'modal-xl'
    }
    
    /**
    * generate HTML <div>s which presents 2 images (Back and Forth) in parallel format
    */
    @Override
    void generateImgTags(Material mate) {
        println "${this.getClass().getName()}#generateImgTags(${mate}) was invoked"
        if (this.comparisonResultBundle_ != null &&
            this.comparisonResultBundle_.containsImageDiff(mate.getPath())) {
            // This material is a diff image, so render it in Carousel format of Diff > Expected + Actual
            ComparisonResult cr = comparisonResultBundle_.get(mate.getPath())
            Path repoRoot = mate.getParent().getParent().getParent().getBaseDir()
            mkbuilder_.div(['class':'carousel slide', 'data-ride':'carousel', 'id': "${mate.hashCode()}carousel"]) {
                mkbuilder_.div(['class':'carousel-inner']) {
                    mkbuilder_.div(['class':'carousel-item']) {
                        mkbuilder_.div(['class':'carousel-caption d-none d-md-block']) {
                            String eval = (cr.imagesAreSimilar()) ? "Images are similar." : "Images are different."
                            String rel = (cr.getDiffRatio() <= cr.getCriteriaPercentage()) ? '<=' : '>'
                            mkbuilder_.p "${eval} diffRatio(${cr.getDiffRatio()}) ${rel} criteria(${cr.getCriteriaPercentage()})"
                        }
                        mkbuilder_.img(['src': "${cr.getDiffMaterial().getEncodedHrefRelativeToRepositoryRoot()}",
                            'class': 'img-fluid d-block mx-auto',
                            'style': 'border: 1px solid #ddd',
                            'alt' : "Diff"])
                    }
                    mkbuilder_.div(['class':'carousel-item active']) {
                        mkbuilder_.div(['class':'carousel-caption d-none d-md-block']) {
                            mkbuilder_.p "Expected: ${cr.getExpectedMaterial().getDescription() ?: ''}" +
                                        " / " +
                                        "Actual: ${cr.getActualMaterial().getDescription() ?: ''}"
                            mkbuilder_.div(['class':'container-fluid']) {
                                mkbuilder_.div(['class':'row']) {
                                    mkbuilder_.div(['class':'col']) {
                                        mkbuilder_.p "Expected ${cr.getExpectedMaterial().getDescription() ?: ''}"
                                        mkbuilder_.img(['src': "${cr.getExpectedMaterial().getEncodedHrefRelativeToRepositoryRoot()}",
                                            'class': 'img-fluid d-block mx-auto',
                                            'style': 'border: 1px solid #ddd',
                                            'alt' : "Expected"])
                                    }
                                    mkbuilder_.div(['class':'col']) {
                                        mkbuilder_.p "Actual ${cr.getActualMaterial().getDescription() ?: ''}"
                                        mkbuilder_.img(['src': "${cr.getActualMaterial().getEncodedHrefRelativeToRepositoryRoot()}",
                                            'class': 'img-fluid d-block mx-auto',
                                            'style': 'border: 1px solid #ddd',
                                            'alt' : "Actual"])
                                    }
                                }
                            }
                        }
                    }
                    mkbuilder_.a(['class':'carousel-control-prev',
                            'href':"#${mate.hashCode()}carousel",
                            'role':'button',
                            'data-slide':'prev']) {
                        mkbuilder_.span(['class':'carousel-control-prev-icon',
                            'area-hidden':'true'], '')
                        mkbuilder_.span(['class':'sr-only'], 'Back')
                    }
                    mkbuilder_.a(['class':'carousel-control-next',
                            'href':"#${mate.hashCode()}carousel",
                            'role':'button',
                            'data-slide':'next']) {
                        mkbuilder_.span(['class':'carousel-control-next-icon',
                            'area-hidden':'true'], '')
                        mkbuilder_.span(['class':'sr-only'], 'Forth')
                    }
                }
            }
        } else {
            mkbuilder_.img(['src': mate.getEncodedHrefRelativeToRepositoryRoot(),
                'class':'img-fluid', 'style':'border: 1px solid #ddd', 'alt':'material'])
        }
    }
}
