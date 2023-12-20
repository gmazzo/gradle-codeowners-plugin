package org.test.lib;

import org.junit.Test;
import org.test.utils.LibUtils;
import org.test.utils.more.MoreUtils;

import static io.github.gmazzo.codeowners.CodeOwners.getCodeOwners;

import static kotlin.collections.SetsKt.setOf;
import static org.junit.Assert.assertEquals;

public class LibOwnersTest {

    @Test
    public void ownerOfLib() {
        assertEquals(setOf("libs-devs"), getCodeOwners(LibClass.class));
    }

    @Test
    public void ownerOfUtils() {
        assertEquals(setOf("libs-devs"), getCodeOwners(LibUtils.class));
    }

    @Test
    public void ownerOfMoreUtils() {
        assertEquals(setOf("utils-devs"), getCodeOwners(MoreUtils.class));
    }

}
