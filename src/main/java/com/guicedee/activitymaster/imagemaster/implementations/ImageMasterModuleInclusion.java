package com.guicedee.activitymaster.imagemaster.implementations;

import com.guicedee.client.services.config.IGuiceScanModuleInclusions;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ImageMasterModuleInclusion implements IGuiceScanModuleInclusions<ImageMasterModuleInclusion>
{
	@Override
	public @NotNull Set<String> includeModules()
	{
		Set<String> set = new HashSet<>();
		set.add("com.guicedee.activitymaster.imagemaster");
		return set;
	}
}

