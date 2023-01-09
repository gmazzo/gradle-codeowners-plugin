package org.test.lib;

import org.junit.Test;
import org.test.utils.LibUtils;

import static io.github.gmazzo.codeowners.CodeOwners.getCodeOwners;

import static kotlin.collections.SetsKt.setOf;
import static org.junit.Assert.assertEquals;

public class LibOwnersTest {

    @Test
    public void ownerOfLib() {
        assertEquals(setOf("kotlin-devs"), getCodeOwners(LibClass.class));
    }

    @Test
    public void ownerOfUtils() {
        assertEquals(setOf("kotlin-devs"), getCodeOwners(LibUtils.class));
    }

}
