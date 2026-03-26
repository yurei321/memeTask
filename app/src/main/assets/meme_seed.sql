-- Seed table for memes with image links and tags.
-- Image links are stored as relative app resource paths.

CREATE TABLE IF NOT EXISTS memes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    image_file TEXT NOT NULL UNIQUE,
    image_link TEXT NOT NULL,
    title TEXT NOT NULL,
    tags_json TEXT NOT NULL
);

INSERT OR IGNORE INTO memes (image_file, image_link, title, tags_json) VALUES
('cat_illsurvive.jpg', 'assets/toMoveToDb/cat_illsurvive.jpg', 'Ничего, выживу', '["кот","слезы","самоирония","стресс","выгорание","держусь"]'),
('2352ffb85675fc9cfa3d9285aef180e2.jpg', 'assets/toMoveToDb/2352ffb85675fc9cfa3d9285aef180e2.jpg', 'Вайба не ощущаю', '["собака","плюшевая игрушка","апатия","нет настроения","без вайба"]'),
('mushrooms_understand.jpg', 'assets/toMoveToDb/mushrooms_understand.jpg', 'Грибы понимают меня', '["грибы","осень","природа","самоирония","чувства","странный юмор"]'),
('dog_itried.jpg', 'assets/toMoveToDb/dog_itried.jpg', 'Я хотя бы попытался', '["щенок","грязь","попытка","провал","мотивация","самоирония"]'),
('3193163d8a228705c627fa9ae1ae0515.jpg', 'assets/toMoveToDb/3193163d8a228705c627fa9ae1ae0515.jpg', 'Иногда думаю, иногда плачу', '["чихуахуа","эмоции","грусть","перепады настроения","тревога"]'),
('pigeon_prime.jpg', 'assets/toMoveToDb/pigeon_prime.jpg', 'В прайме', '["чайка","роза","абсурд","самоуверенность","ирония"]'),
('4590fe6d55fc511c14893b35f6e17e47.jpg', 'assets/toMoveToDb/4590fe6d55fc511c14893b35f6e17e47.jpg', 'Наверно все будет норм', '["барашек","надежда","тревожность","поддержка","мягкий юмор"]'),
('sunset_nomore.jpg', 'assets/toMoveToDb/sunset_nomore.jpg', 'Спасибо за опыт, больше не надо', '["рассвет","урок","выгорание","никогда снова","самоирония"]'),
('54bd9c26c4b46d1f79bca54649a220f4.jpg', 'assets/toMoveToDb/54bd9c26c4b46d1f79bca54649a220f4.jpg', 'Ура, я рад без причины', '["радость","эйфория","неожиданное счастье","позитив","мем"]'),
('55b4c17c31cc39011918ec2eb3fd1208.jpg', 'assets/toMoveToDb/55b4c17c31cc39011918ec2eb3fd1208.jpg', 'Могу все, но со слезами', '["котенок","слезы","усталость","стойкость","самоирония"]'),
('594978ba0ab92e1345ed56f3aba9c9a0.jpg', 'assets/toMoveToDb/594978ba0ab92e1345ed56f3aba9c9a0.jpg', 'Это выносимо', '["хомяк","терпение","стресс","цензура","держусь","ирония"]'),
('5beb74262bfb1f88aaa95d6c113175ce.jpg', 'assets/toMoveToDb/5beb74262bfb1f88aaa95d6c113175ce.jpg', 'Что делать?', '["рыба","книга","учеба","растерянность","вопрос","абсурд"]'),
('676c5f1134b91a37b3828520ff3b4dab.jpg', 'assets/toMoveToDb/676c5f1134b91a37b3828520ff3b4dab.jpg', 'Беда', '["рыба","книга","проблема","паника","минимализм","ирония"]'),
('76aea08c0c36b8d5968a4acd4eb41725.jpg', 'assets/toMoveToDb/76aea08c0c36b8d5968a4acd4eb41725.jpg', 'Совсем облинился', '["блин","еда","сонливость","усталость","лень","каламбур"]'),
('77e32c651e9f37d35d82935429a6dc01.jpg', 'assets/toMoveToDb/77e32c651e9f37d35d82935429a6dc01.jpg', 'Терпение и труд... перекур', '["природа","чай","прокрастинация","работа","перерыв","ирония"]'),
('7b43d30cbaa55aabbe2dd7ac82ae36db.jpg', 'assets/toMoveToDb/7b43d30cbaa55aabbe2dd7ac82ae36db.jpg', 'Ультрабессилие', '["котенок","сон","истощение","нет сил","усталость"]'),
('9772321a4bd9dd3f6ccd04921e4e0e0a.jpg', 'assets/toMoveToDb/9772321a4bd9dd3f6ccd04921e4e0e0a.jpg', 'Праймовые мувы', '["кот","булочки","самоуверенность","шутка","милота"]'),
('ae76f0e1b728c0b11caa51b0f9f6d049.jpg', 'assets/toMoveToDb/ae76f0e1b728c0b11caa51b0f9f6d049.jpg', 'Сегодня не приду, не вайбово', '["социофобия","нет ресурса","апатия","вайб","самоирония"]'),
('bd838cd5cb02b4ca72ecb41deb053354.jpg', 'assets/toMoveToDb/bd838cd5cb02b4ca72ecb41deb053354.jpg', 'Я существую, чтобы прикалываться', '["кот","юмор","беззаботность","самоирония","позитив"]'),
('c12f151f5e390190f28445d776b563c9.jpg', 'assets/toMoveToDb/c12f151f5e390190f28445d776b563c9.jpg', 'Делу время, а потехехехехе', '["котенок","прокрастинация","смешно","баланс работа отдых","ирония"]'),
('c49e81c953740c66784672743f6c1d0d.jpg', 'assets/toMoveToDb/c49e81c953740c66784672743f6c1d0d.jpg', 'Гриб одобряет усилия', '["гриб","одобрение","поддержка","мотивация","мем"]'),
('d0809a472d57c0743cf2e00d34b16410.jpg', 'assets/toMoveToDb/d0809a472d57c0743cf2e00d34b16410.jpg', 'Выбираю спокойствие', '["спокойствие","цветы","гармония","осознанность","уют"]'),
('d316898df628dc98297863d84bca50d1.jpg', 'assets/toMoveToDb/d316898df628dc98297863d84bca50d1.jpg', 'Не сдавайся, отдохни и продолжай', '["кот","поддержка","антистресс","мотивация","забота"]'),
('dfdb105e6a90348963d2cc7d3166ac5e.jpg', 'assets/toMoveToDb/dfdb105e6a90348963d2cc7d3166ac5e.jpg', 'Мне очень нравится жить', '["кот","радость","жизнелюбие","позитив","весна"]'),
('ec6fc42d36a9a215e70a9194e40025c3.jpg', 'assets/toMoveToDb/ec6fc42d36a9a215e70a9194e40025c3.jpg', 'Вайб утерян', '["собака","грусть","апатия","без настроения","вайб"]');
