/**
 * 
 */
package com.tui.image.processor.service.impl;

import de.hybris.platform.catalog.CatalogVersionService;
import de.hybris.platform.catalog.model.CatalogVersionModel;
import de.hybris.platform.core.Registry;
import de.hybris.platform.core.model.media.MediaContainerModel;
import de.hybris.platform.core.model.media.MediaFolderModel;
import de.hybris.platform.core.model.media.MediaFormatModel;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.jalo.media.MediaContainer;
import de.hybris.platform.servicelayer.media.impl.DefaultMediaService;
import de.hybris.platform.servicelayer.model.ModelService;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import com.tui.image.processor.service.ImageConverterService;


/**
 * @author Madan80
 * 
 */
public class ImageConverterServiceImpl implements ImageConverterService
{
	@Autowired
	private DefaultMediaService mediaService;
	@Autowired
	private ModelService modelService;
	@Autowired
	private CatalogVersionService catalogVersionService;


	private MediaFormatModel mediaFormatModel;
	private MediaContainer mediaContainer;
	private MediaModel mediaModel;
	private final Map<String, String> mapImagePath = new HashMap<String, String>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.tui.image.processor.service.ImageConverterService#createMedia(java.lang.String)
	 */
	@Override
	public Map<String, String> createMedia(final MultipartFile file, final String name)
	{
		if (!mapImagePath.isEmpty())
		{
			mapImagePath.clear();
		}
		/*
		 * final String mediaURl = Registry.getCurrentTenant().getConfig().getParameter("media.server.url") + "/sys_" +
		 * Registry.getCurrentTenant().getTenantID() + "/test";
		 */
		//write image to filesystem and create cropped and resized image
		final String[] str = name.split("\\.");
		final String imageName = str[0];
		final String ext = str[1];
		final String mediaLoc = Registry.getCurrentTenant().getConfig().getParameter("mediaweb.webroot") + File.separator + "sys_"
				+ Registry.getCurrentTenant().getTenantID() + "/test";

		final MediaFolderModel mediaFolderModel = modelService.create(MediaFolderModel.class);
		mediaFolderModel.setQualifier(imageName);
		/*
		 * mediaFolderModel.setPath(mediaService.getFolder("sys_" + Registry.getCurrentTenant().getTenantID()).getPath() +
		 * File.separator + name.split("\\.")[0]);
		 */
		modelService.save(mediaFolderModel);

		//final MediaFolderModel configFolderModel = mediaService.getFolder(imageName);
		//final String mediaFolderPath = configFolderModel.getPath();
		//System.out.println("mediaFolderPath : " + mediaFolderPath);
		processImage(file, imageName, ext, mediaLoc);
		final CatalogVersionModel catlogVersion = catalogVersionService.getCatalogVersion("SEBlueContentCatalog", "Staged");
		final MediaContainerModel mediaContainer = modelService.create(MediaContainerModel.class);
		mediaContainer.setQualifier(name.split("\\.")[0]);
		mediaContainer.setCatalogVersion(catlogVersion);

		for (final Entry<String, String> entry : mapImagePath.entrySet())
		{
			final String imgPath = entry.getValue();
			final String mediaName = imgPath.substring(imgPath.lastIndexOf("\\") + 1);
			final MediaFormatModel format = modelService.create(MediaFormatModel.class);
			format.setQualifier(entry.getKey());

			final MediaModel mediaModel = modelService.create(MediaModel.class);
			mediaModel.setCode(entry.getKey());
			mediaModel.setMediaFormat(format);
			mediaModel.setMediaContainer(mediaContainer);
			mediaModel.setLocation(entry.getValue());
			mediaModel.setMime("image/jpeg");
			mediaModel.setFolder(mediaFolderModel);
			mediaModel.setRealFileName(mediaName);
			final File mediaFile = new File(entry.getValue());

			InputStream mediaInputStream = null;
			try
			{
				mediaInputStream = new FileInputStream(mediaFile);
			}
			catch (final FileNotFoundException e)
			{
				e.printStackTrace();
			}


			mediaModel.setCatalogVersion(catlogVersion);
			mediaModel.setHight("640");
			mediaModel.setWidth("480");
			modelService.save(mediaModel);
			mediaService.setStreamForMedia(mediaModel, mediaInputStream, mediaName, "image/jpeg");
			final String url = mediaService.getMedia(entry.getKey()).getDownloadURL();
			System.out.println("URL is::::::::::::::  " + url);
		}
		System.out.println("Media created and saved");
		// YTODO Auto-generated method stub
		return null;
	}

	/**
	 * @param file
	 * @param name
	 */
	private void processImage(final MultipartFile file, final String name, final String ext, final String mediaLoc)
	{
		if (!file.isEmpty())
		{
			try
			{
				final byte[] bytes = file.getBytes();

				// Creating the directory to store file

				final File dir = new File(mediaLoc);
				if (!dir.exists())
				{
					dir.mkdirs();
				}

				// Create the file on server
				final File serverFile = new File(dir.getAbsolutePath() + File.separator + name + "." + ext);
				mapImagePath.put(name, serverFile.getAbsolutePath());
				final BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile));
				stream.write(bytes);
				stream.close();

				cropUploadedImageXS(serverFile, name, ext);
				resizeUploadedImageXS(serverFile, name, ext);
				resizeUploadedImageSM(serverFile, name, ext);
				resizeUploadedImageMD(serverFile, name, ext);
				resizeUploadedImageLG(serverFile, name, ext);



			}
			catch (final Exception e)
			{
				System.out.println(e.getMessage());
			}
		}
		else
		{
			System.out.println("You failed to upload " + name + " because the file was empty.");
		}

	}

	private void resizeUploadedImageSM(final File serverFile, final String name, final String ext) throws IOException
	{

		final String imageName = serverFile.getAbsolutePath();
		final String[] splitedName = imageName.split("\\.");
		//final StringBuilder cmdBuilder = new StringBuilder();
		final String resizedImage = splitedName[0] + "_sm." + splitedName[1];
		final String[] cmdResize = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-resize", "489x320^", "-quality", "80", "+repage", resizedImage };


		Runtime.getRuntime().exec(cmdResize);
		mapImagePath.put(name.split("\\.")[0] + "_sm", resizedImage);

		final String resizedImageHD = splitedName[0] + "_sm_hd." + splitedName[1];
		final String[] cmdResizeHD = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-resize", "489x320^", "-quality", "100", "+repage", resizedImageHD };


		Runtime.getRuntime().exec(cmdResizeHD);
		mapImagePath.put(name.split("\\.")[0] + "_sm" + "_hd", resizedImageHD);

		System.out.println("Image resized");


	}

	private void resizeUploadedImageXS(final File serverFile, final String name, final String ext) throws IOException
	{

		final String imageName = serverFile.getAbsolutePath();
		final String[] splitedName = imageName.split("\\.");
		//final StringBuilder cmdBuilder = new StringBuilder();
		final String resizedImage = splitedName[0] + "_xs." + splitedName[1];
		final String[] cmdResize = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-resize", "306x204^", "-quality", "80", "+repage", resizedImage };


		Runtime.getRuntime().exec(cmdResize);
		mapImagePath.put(name.split("\\.")[0] + "_xs", resizedImage);

		final String resizedImageHD = splitedName[0] + "_xs_hd." + splitedName[1];

		final String[] cmdResizeHD = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-resize", "306x204^", "-quality", "100", "+repage", resizedImageHD };


		Runtime.getRuntime().exec(cmdResizeHD);
		mapImagePath.put(name.split("\\.")[0] + "_xs" + "_hd", resizedImageHD);

		System.out.println("Image resized");


	}

	private void resizeUploadedImageMD(final File serverFile, final String name, final String ext) throws IOException
	{

		final String imageName = serverFile.getAbsolutePath();
		final String[] splitedName = imageName.split("\\.");
		//final StringBuilder cmdBuilder = new StringBuilder();
		final String resizedImage = splitedName[0] + "_md." + splitedName[1];
		final String[] cmdResize = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-resize", "640x480^", "-quality", "80", "+repage", resizedImage };


		Runtime.getRuntime().exec(cmdResize);
		mapImagePath.put(name.split("\\.")[0] + "_md", resizedImage);

		final String resizedImageHD = splitedName[0] + "_md_hd." + splitedName[1];

		final String[] cmdResizeHD = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-resize", "640x480^", "-quality", "100", "+repage", resizedImageHD };



		Runtime.getRuntime().exec(cmdResizeHD);
		mapImagePath.put(name.split("\\.")[0] + "_md" + "_hd", resizedImageHD);

		System.out.println("Image resized");


	}

	private void resizeUploadedImageLG(final File serverFile, final String name, final String ext) throws IOException
	{

		final String imageName = serverFile.getAbsolutePath();
		final String[] splitedName = imageName.split("\\.");
		//final StringBuilder cmdBuilder = new StringBuilder();
		final String resizedImage = splitedName[0] + "_lg." + splitedName[1];
		final String[] cmdResize = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-resize", "800x523^", "-quality", "80", "+repage", resizedImage };


		Runtime.getRuntime().exec(cmdResize);
		mapImagePath.put(name.split("\\.")[0] + "_lg", resizedImage);

		final String resizedImageHD = splitedName[0] + "_lg_hd." + splitedName[1];

		final String[] cmdResizeHD = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-resize", "800x523^", "-quality", "100", "+repage", resizedImageHD };


		Runtime.getRuntime().exec(cmdResizeHD);
		mapImagePath.put(name.split("\\.")[0] + "_lg" + "_hd", resizedImageHD);

		System.out.println("Image resized");


	}

	private void cropUploadedImageXS(final File serverFile, final String name, final String ext) throws IOException
	{

		final String imageName = serverFile.getAbsolutePath();
		final String[] splitedName = imageName.split("\\.");
		final String croppedImage = splitedName[0] + "_xs_cropped." + splitedName[1];
		final String[] cmdCrop = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-gravity", "center", "-crop", "306x204-20+20", "-quality", "80",
				"+repage", croppedImage };

		Runtime.getRuntime().exec(cmdCrop);
		mapImagePath.put(name.split("\\.")[0] + "_xs_cropped", croppedImage);

		final String resizedImageHD = splitedName[0] + "_xs_cropped_hd." + splitedName[1];

		final String[] cmdCropHD = new String[]
		{ "C:\\ImageMagick-6.9.1-Q16\\convert.exe", imageName, "-gravity", "center", "-crop", "306x204-20+20", "-quality", "100",
				"+repage", resizedImageHD };

		Runtime.getRuntime().exec(cmdCropHD);
		mapImagePath.put(name.split("\\.")[0] + "_xs_cropped_hd", resizedImageHD);

		System.out.println("Image cropped");

	}

	/**
	 * @param mediaService
	 *           the mediaService to set
	 */
	public void setMediaService(final DefaultMediaService mediaService)
	{
		this.mediaService = mediaService;
	}
}
