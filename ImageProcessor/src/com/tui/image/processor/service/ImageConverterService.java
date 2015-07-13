/**
 * 
 */
package com.tui.image.processor.service;

import java.util.Map;

import org.springframework.web.multipart.MultipartFile;


/**
 * @author Madan80
 * 
 */
public interface ImageConverterService
{
	public Map<String, String> createMedia(MultipartFile file, String name);

}
