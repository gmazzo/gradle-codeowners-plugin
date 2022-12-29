package org.test.lib;

import org.junit.Test;
import org.test.utils.LibUtils;

import static java.util.Collections.singletonList;

import static com.github.gmazzo.codeowners.CodeOwners.getCodeOwners;

import static org.junit.Assert.assertEquals;

public class LibOwnersTest {

    @Test
    public void ownerOfLib() {
        assertEquals(singletonList("kotlin-devs"), getCodeOwners(LibClass.class));
    }

    @Test
    public void ownerOfUtils() {
        assertEquals(singletonList("kotlin-devs"), getCodeOwners(LibUtils.class));
    }

}
