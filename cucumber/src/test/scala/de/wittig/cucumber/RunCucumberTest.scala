package de.wittig.cucumber

import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectPackages
import org.junit.platform.suite.api.Suite

import io.cucumber.core.options.Constants.GLUE_PROPERTY_NAME

@Suite
@IncludeEngines(Array("cucumber"))
@SelectPackages(Array("de.wittig.cucumber"))
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "de.wittig.cucumber")
class RunCucumberTest
