import Image from "next/image";
import { photos, type PhotoKey } from "@/content/photos";

type Props = {
  photo: PhotoKey;
  /** Tells the browser how wide the photo shows, so it downloads the right size. */
  sizes: string;
  priority?: boolean;
  position?: string;
  /** Scroll effect applied to the image: "settle" zooms in on entry, "drift" moves slower than the page. */
  motion?: "settle" | "drift" | "none";
};

/** A photo that fills its positioned parent. The dark surface shows while it loads. */
export function PhotoFill({ photo, sizes, priority = false, position = "center", motion = "settle" }: Props) {
  const { src, alt } = photos[photo];
  return (
    <div className={`absolute inset-0 overflow-hidden bg-surface ${motion === "none" ? "" : motion}`}>
      <Image
        src={src}
        alt={alt}
        fill
        sizes={sizes}
        preload={priority}
        className="object-cover"
        style={{ objectPosition: position }}
      />
    </div>
  );
}
