package com.tui.image.processor.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.tui.image.processor.service.ImageConverterService;


/**
 * Handles requests for the application file upload requests
 */
@Controller
public class FileUploadController
{
	@Autowired
	private ImageConverterService imageConverterService;
	private static final Logger logger = LoggerFactory.getLogger(FileUploadController.class);

	/**
	 * Upload single file using Spring Controller
	 */
	@RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
	public @ResponseBody
	String uploadFileHandler(@RequestParam("name") final String name, @RequestParam("file") final MultipartFile file)
	{
		imageConverterService.createMedia(file, name);

		if (!file.isEmpty())
		{
			try
			{
				final byte[] bytes = file.getBytes();

				// Creating the directory to store file

				final File dir = new File("D:\\uploadedImage");
				if (!dir.exists())
				{
					dir.mkdirs();
				}

				// Create the file on server
				final File serverFile = new File(dir.getAbsolutePath() + File.separator + name);
				final BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
				stream.write(bytes);
				stream.close();
				System.setProperty("jmagick.systemclassloader", "no");
				//	cropUploadedImageXS(serverFile);
				//	resizeUploadedImageSM(serverFile);
				logger.info("Server File Location=" + serverFile.getAbsolutePath());

				return "You successfully uploaded file=" + name;
			}
			catch (final Exception e)
			{
				return "You failed to upload " + name + " => " + e.getMessage();
			}
		}
		else
		{
			return "You failed to upload " + name + " because the file was empty.";
		}
	}

	private void resizeUploadedImageSM(final File serverFile) throws IOException
	{

		final String imageName = serverFile.getAbsolutePath();
		final String[] splitedName = imageName.split("\\.");
		//final StringBuilder cmdBuilder = new StringBuilder();
		final String[] cmdResize = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-resize", "500x500^", splitedName[0] + "_sm." + splitedName[1] };


		Runtime.getRuntime().exec(cmdResize);

		System.out.println("Image resized");


	}

	private void cropUploadedImageXS(final File serverFile) throws IOException
	{

		final String imageName = serverFile.getAbsolutePath();
		final String[] splitedName = imageName.split("\\.");

		final String[] cmdCrop = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-gravity", "center", "-crop", "100x100+10+10", "+repage",
				splitedName[0] + "_xs." + splitedName[1] };

		Runtime.getRuntime().exec(cmdCrop);

		System.out.println("Image cropped");

	}

	/**
	 * Upload multiple file using Spring Controller
	 */
	@RequestMapping(value = "/uploadMultipleFile", method = RequestMethod.POST)
	public @ResponseBody
	String uploadMultipleFileHandler(@RequestParam("name") final String[] names, @RequestParam("file") final MultipartFile[] files)
	{

		if (files.length != names.length)
		{
			return "Mandatory information missing";
		}

		String message = "";
		for (int i = 0; i < files.length; i++)
		{
			final MultipartFile file = files[i];
			final String name = names[i];
			try
			{
				final byte[] bytes = file.getBytes();

				// Creating the directory to store file
				final String rootPath = System.getProperty("catalina.home");
				final File dir = new File(rootPath + File.separator + "tmpFiles");
				if (!dir.exists())
				{
					dir.mkdirs();
				}

				// Create the file on server
				final File serverFile = new File(dir.getAbsolutePath() + File.separator + name);
				final BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
				stream.write(bytes);
				stream.close();

				logger.info("Server File Location=" + serverFile.getAbsolutePath());

				message = message + "You successfully uploaded file=" + name + "<br />";
			}
			catch (final Exception e)
			{
				return "You failed to upload " + name + " => " + e.getMessage();
			}
		}
		return message;
	}

	@RequestMapping("/")
	public String renderFileUploadView()
	{
		return "fileUpload";
	}
}
